package creditcardFinecalculator;

/**************************************************************************************************/ /* *

DO NOT ADD PACKAGE DECLARATION
PLEASE NOTE: THIS SKELETON FILE MUST BE PLACED UNDER DEFAULT PACKAGE OF SRC FOLDER
ALL THE CODING MUST BE DONE INSIDE THIS SINGLE .JAVA FILE ONLY
DO NOT CREATE DEPENDENT CLASSES
DO NOT MODIFY CODE SKELETON
DO NOT MODIFY:
 ACCESS SPECIFIERS, RETURN TYPES OR DATA TYPES, EXCEPTION CLAUSES, 
 CLASS OR METHOD NAMES IN THE SKELETON
YOU MUST CODE ON THE SAME PROJECT MAPPED IN EBOX VIEW THROUGHOUT THE ASSESSMENT
CLICK ON 'Problem/Project Status' BUTTON TO SEE THE PROJECT MAPPED IN EBOX
YOU MUST CLICK ON 'SAVE PROJECT' BUTTON EVERY 10 MINUTES TO PERIODICALLY SAVE
CTRL + S ALONE, WILL NOT BE SUFFICIENT TO SAVE YOUR CODE IN EBOX
CLICKING ON 'SAVE PROJECT' ENSURES NO LOSS OF CODE
REACH OUT TO PROCTORS FOR ANY QUERIES
Skeleton Version 2.0
Date Modified: 21-June-2016 / /************************************************************************************************/
// imports here

import java.io.BufferedReader; 
import java.io.FileNotFoundException;
import java.io.FileReader; 
import java.io.IOException;
import java.text.Format;
import java.text.ParseException; 
import java.text.SimpleDateFormat;
import java.util.ArrayList; 
import java.util.Date; 
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**

This class must be used to write the solution for the given requirement. No
additional classes must be created
**/ 
public class CreditCardFineCalculator { /* * @param filePath * @return Map<Integer, Map> * @throws CreditCardFineCalculatorException * * */ 
public Map<Integer, Map> getCreditCardFineDetails(String filePath) throws CreditCardFineCalculatorException { List<String[]> creditCardsDetails = readFromInputFile(filePath); if (creditCardsDetails == null || creditCardsDetails.isEmpty()) { throw new CreditCardFineCalculatorException("File is empty"); } Map<Integer, Map> cardResultMap = validateInputsAndPopulate(creditCardsDetails); // TODO Associate to type their code here // TODO Associate to modify the return statement according to the // requirement

	return cardResultMap; // TODO Change this return value
}

private List<String[]> readFromInputFile(final String filePath)
		throws CreditCardFineCalculatorException {
	List<String[]> inputDataList = new ArrayList<String[]>();
	try {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String singleLineFromInput = null;

		while ((singleLineFromInput = br.readLine()) != null) {
			if (singleLineFromInput != null
					&& singleLineFromInput.length() > 0) {
				String[] wordsInSingleLine = singleLineFromInput.split("\\|"); // this
				// is
				// special
				inputDataList.add(wordsInSingleLine);
			}
		}

	} catch (FileNotFoundException e) {
		throw new CreditCardFineCalculatorException("Input File Not Found");
	} catch (IOException e) {
		throw new CreditCardFineCalculatorException(
				"Input Output Exception");
	}
	return inputDataList;
}

private Map<Integer, Map> validateInputsAndPopulate(
		final List<String[]> inputDataList)
		throws CreditCardFineCalculatorException {
	Map<String, CreditCardVO> visaMap = new HashMap<String, CreditCardVO>();
	Map<String, CreditCardVO> amaxMap = new HashMap<String, CreditCardVO>();
	Map<Integer, Map> resultMap = new HashMap<Integer, Map>();
	CreditCardVO vo = new CreditCardVO();
	String amexCardNo = null; // 16 digit
	String visaCardNo = null; // 14 or 15 digit starting with 4 or 5
	Date dueDate = null;
	Date billPaidDate = null;
	boolean dataFromFileIsOK = true;
	int billAmount = 0;
	boolean isVisaCard = false;
	for (String[] singleRowOfInput : inputDataList) {
		if (singleRowOfInput == null || singleRowOfInput.length < 5) {
			throw new CreditCardFineCalculatorException(
					"Input File Invalid");
		}
		if (singleRowOfInput[0].isEmpty() || singleRowOfInput[1].isEmpty()
				|| singleRowOfInput[2].isEmpty()
				|| singleRowOfInput[3].isEmpty()
				|| singleRowOfInput[4].isEmpty()) {
			throw new CreditCardFineCalculatorException(
					"Input File Invalid");
		}

		vo = new CreditCardVO();
		// -------------validate DUE date
		dueDate = validateDate(singleRowOfInput[3]);
		if (dueDate == null) {
			dataFromFileIsOK = false; // discard the card
			throw new CreditCardFineCalculatorException(
					"DueDate is null for Card " + vo.getCreditCardNumber());
		}

		// -------------validate PAID date
		billPaidDate = validateDate(singleRowOfInput[4]);
		if (billPaidDate == null) {
			dataFromFileIsOK = false; // discard the card
			throw new CreditCardFineCalculatorException(
					"billPaidDate is null for Card "
							+ vo.getCreditCardNumber());
		}

		// ---------- calculate the amount to be paid
		if (!isDigit(singleRowOfInput[2])) {
			throw new CreditCardFineCalculatorException("Invalid amount");
		}
		billAmount = Integer.parseInt(singleRowOfInput[2]);
		if (billAmount < 0) {
			dataFromFileIsOK = false; // discard the card
			throw new CreditCardFineCalculatorException(
					"billAmount is less than 0 for Card "
							+ vo.getCreditCardNumber());
		}
		String customerName = singleRowOfInput[1];
		if (dataFromFileIsOK) {
			vo.setCustomerName(customerName);
			vo.setDueDate(dueDate);
			vo.setPaymentDate(billPaidDate);
			vo.setBillAmount(billAmount);
		}
		if (!isDigit(singleRowOfInput[0])) {
			throw new CreditCardFineCalculatorException("Invalid card no");
		}
		String firstDigit = singleRowOfInput[0].substring(0, 1);
		String twoDigit = singleRowOfInput[0].substring(0, 2);
		if (singleRowOfInput[0] != null
				&& singleRowOfInput[0].length() == 15
				&& (twoDigit.equals("34") || twoDigit.equals("37"))) {
			amexCardNo = singleRowOfInput[0].substring(0,
					singleRowOfInput[0].length());
			isVisaCard = false;
			vo.setCreditCardNumber(amexCardNo);
			if (delayedPayment(vo.getDueDate(), vo.getPaymentDate())) {
				char grade = determineGrade(vo.getDueDate(),
						vo.getPaymentDate());
				vo.setCreditCardGrade(grade);
				int fine = determineFineForAamax(vo.getDueDate(),
						vo.getPaymentDate(), vo.getBillAmount());
				vo.setFine(fine);
			} else {
				vo.setFine(0);
				vo.setCreditCardGrade('A');// if there is no fine, then
											// please put the
				// // // value as 0
			}

		} else if (singleRowOfInput[0] != null
				&& singleRowOfInput[0].length() == 16
				&& firstDigit.equals("4")) {
			visaCardNo = singleRowOfInput[0].substring(0,
					singleRowOfInput[0].length());
			isVisaCard = true;
			vo.setCreditCardNumber(visaCardNo);
			if (delayedPayment(vo.getDueDate(), vo.getPaymentDate())) {
				char grade = determineGrade(vo.getDueDate(),
						vo.getPaymentDate());
				vo.setCreditCardGrade(grade);
				// System.out.println("Grade::::::" + vo.getGrade());
				int fine = determineFineForVisa(vo.getDueDate(),
						vo.getPaymentDate(), vo.getBillAmount());
				vo.setFine(fine);
			} else {
				vo.setFine(0); // if there is no fine, then please put the
				vo.setCreditCardGrade('A'); // // value as 0.0
			}

		} else {
			isVisaCard = false;
			dataFromFileIsOK = false; // discard the card
			throw new CreditCardFineCalculatorException(
					"Invalid Credit Card Number "
							+ singleRowOfInput[0].substring(0,
									singleRowOfInput[0].length()));
		}

		if (isVisaCard) {
			if (visaMap.get(vo.getCreditCardNumber()) != null) {
				CreditCardVO cardVO = visaMap.get(vo.getCreditCardNumber());
				if (checkLatestDate(cardVO.getPaymentDate(),
						vo.getPaymentDate())) {
					visaMap.put(vo.getCreditCardNumber(), vo);
				}
			} else {
				visaMap.put(vo.getCreditCardNumber(), vo);
			}

		} else {
			if (amaxMap.get(vo.getCreditCardNumber()) != null) {
				CreditCardVO cardVO = amaxMap.get(vo.getCreditCardNumber());
				if (checkLatestDate(cardVO.getPaymentDate(),
						vo.getPaymentDate())) {
					amaxMap.put(vo.getCreditCardNumber(), vo);
				}
			} else {
				amaxMap.put(vo.getCreditCardNumber(), vo);
			}

		}
	}
	resultMap.put(new Integer(1), visaMap);
	resultMap.put(new Integer(2), amaxMap);
	return resultMap;
}

private Date validateDate(final String input)
		throws CreditCardFineCalculatorException {
	Date formattedDate = null;
	if (input != null && input.length() > 1) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		try {
			sdf.setLenient(false);
			formattedDate = sdf.parse(input);
		} catch (ParseException e) {
			throw new CreditCardFineCalculatorException(
					"Parse Exception for Date " + input);
		}
	}
	return formattedDate;
}

private boolean delayedPayment(Date dueDate, Date billPaidDate) {
	if (billPaidDate.after(dueDate)) {
		return true;
	}
	return false;
}

private boolean checkLatestDate(Date oldDate, Date newDate) {
	if (newDate.after(oldDate)) {
		return true;
	}
	return false;
}

private int determineFineForVisa(Date dueDate, Date billPaidDate,
		int billAmount) {
	int diffInDays = (int) ((billPaidDate.getTime() - dueDate.getTime()) / (1000 * 60 * 60 * 24));
	int fine = 0;
	if (diffInDays >= 1 && diffInDays < 5) {
		fine = (int) (billAmount * .10);
	} else if (diffInDays > 5) {
		fine = (int) (billAmount * .20);
	} else {
		fine = 0;
	}
	return fine;
}

private int determineFineForAamax(Date dueDate, Date billPaidDate,
		int billAmount) {
	int diffInDays = (int) ((billPaidDate.getTime() - dueDate.getTime()) / (1000 * 60 * 60 * 24));
	int fine = 0;
	if (diffInDays >= 1 && diffInDays < 5) {
		fine = (int) (billAmount * .10);
	} else if (diffInDays > 5) {
		if (billAmount <= 15000) {
			fine = (int) (billAmount * .20);
		} else {
			fine = (int) (billAmount * .30);
		}
	} else {
		fine = 0;
	}
	return fine;
}

private char determineGrade(Date dueDate, Date billPaidDate) {
	char grade = 'A';
	int diffInDays = (int) ((billPaidDate.getTime() - dueDate.getTime()) / (1000 * 60 * 60 * 24));
	if (diffInDays <= 0) {
		grade = 'A';
	} else {
		grade = 'B';
	}
	return grade;
}

private boolean isDigit(String number) {
	boolean isDesigt = true;
	try {
		Double.parseDouble(number);
	} catch (Exception e) {
		isDesigt = false;
	}
	return isDesigt;
}
}

/**

VO class supplied part of the Skeleton. Do not modify this class
*/ class CreditCardVO {

private String creditCardNumber;
private int billAmount;
private int fine;
private Date dueDate;
private Date paymentDate;
private String customerName;
private char creditCardGrade;

/**
 * @return the creditCardNumber
 */
public String getCreditCardNumber() {
	return creditCardNumber;
}

/**
 * @param creditCardNumber
 *            the creditCardNumber to set
 */
public void setCreditCardNumber(String creditCardNumber) {
	this.creditCardNumber = creditCardNumber;
}

/**
 * @return the billAmount
 */
public int getBillAmount() {
	return billAmount;
}

/**
 * @param billAmount
 *            the billAmount to set
 */
public void setBillAmount(int billAmount) {
	this.billAmount = billAmount;
}

/**
 * @return the fine
 */
public int getFine() {
	return fine;
}

/**
 * @param fine
 *            the fine to set
 */
public void setFine(int fine) {
	this.fine = fine;
}

/**
 * @return the dueDate
 */
public Date getDueDate() {
	return dueDate;
}

/**
 * @param dueDate
 *            the dueDate to set
 */
public void setDueDate(Date dueDate) {
	this.dueDate = dueDate;
}

/**
 * @return the paymentDate
 */
public Date getPaymentDate() {
	return paymentDate;
}

/**
 * @param paymentDate
 *            the paymentDate to set
 */
public void setPaymentDate(Date paymentDate) {
	this.paymentDate = paymentDate;
}

/**
 * @return the customerName
 */
public String getCustomerName() {
	return customerName;
}

/**
 * @param customerName
 *            the customerName to set
 */
public void setCustomerName(String customerName) {
	this.customerName = customerName;
}

/**
 * @return the creditCardGrade
 */
public char getCreditCardGrade() {
	return creditCardGrade;
}

/**
 * @param creditCardGrade
 *            the creditCardGrade to set
 */
public void setCreditCardGrade(char creditCardGrade) {
	this.creditCardGrade = creditCardGrade;
}

/*
 * (non-Javadoc)
 * 
 * @see java.lang.Object#equals(java.lang.Object)
 */
@Override
public boolean equals(Object obj) {

	CreditCardVO other = (CreditCardVO) obj;
	Format formatter = new SimpleDateFormat("yyyy-MM-dd");

	boolean isEqual = false;

	if (creditCardGrade == other.creditCardGrade
			&& creditCardNumber.equals(other.creditCardNumber)
			&& fine == other.fine
			&& formatter.format(paymentDate).equals(
					formatter.format(other.paymentDate))) {
		isEqual = true;

	}
	return isEqual;
}

/*
 * (non-Javadoc)
 * 
 * @see java.lang.Object#toString()
 */
@Override
public String toString() {
	StringBuilder builder = new StringBuilder();
	builder.append("CreditCardVO [creditCardNumber=");
	builder.append(creditCardNumber);
	builder.append(", billAmount=");
	builder.append(billAmount);
	builder.append(", fine=");
	builder.append(fine);
	builder.append(", dueDate=");
	builder.append(dueDate);
	builder.append(", paymentDate=");
	builder.append(paymentDate);
	builder.append(", customerName=");
	builder.append(customerName);
	builder.append(", creditCardGrade=");
	builder.append(creditCardGrade);
	builder.append("]");
	return builder.toString();
}
}

/** *

Exception class supplied part of the Skeleton. Do not modify this class/* * */
class CreditCardFineCalculatorException extends Exception 
{  private static final long serialVersionUID = 1L;

/**
 * @param message
 */
public CreditCardFineCalculatorException(String message) {
	super();
}

/**
 * @param throwable
 */
public CreditCardFineCalculatorException(Throwable throwable) {
	super(throwable);
}
}