package trustloan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoanTrustInsurance {
	public static void main(String[] args) {
		LoanTrustInsurance loanTrustInsurance = new LoanTrustInsurance();
		String filePath = "input1.txt";
		try {
			System.out.println(loanTrustInsurance.loanTrustDetails(filePath,
					"02/07/2015"));
		} catch (LoanTrustException e) {
			e.printStackTrace();
		}
	}

	public Map<Integer, Map<String, List<LoanTrustVo>>> loanTrustDetails(
			String filePath, String sanctionDate) throws LoanTrustException {
		Map<Integer, Map<String, List<LoanTrustVo>>> retMap = new HashMap<>();
		Map<String, List<LoanTrustVo>> innermap1 = new HashMap<>();
		Map<String, List<LoanTrustVo>> innermap2 = new HashMap<>();
		List<LoanTrustVo> loanTrustList = new ArrayList<>();
		File file = new File(filePath);
		if (!file.exists()) {
			throw new LoanTrustException("File should not be empty");
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = br.readLine()) != null) {
				LoanTrustVo loanTrustVo = new LoanTrustVo();

				String[] loanArray = line.split(";");

				if (loanArray.length != 6) {
					throw new LoanTrustException("All fields are Mandatory");
				}

				for (String str : loanArray) {
					if (str.isEmpty() || str.equals(null)) {
						throw new LoanTrustException(
								"input field should not be a null or empty");
					}
				}

				String[] plcysplit = loanArray[0].split("-");
				Float assuredamt = Float.parseFloat(plcysplit[2]);
				loanTrustVo.setSumAssured(assuredamt);

				if (loanArray[0]
						.matches("((FST)|(NRM))[-]{1}[1-9]+[-]{1}[0-9]+")) {
					loanTrustVo.setPolicyNumber(loanArray[0]);
					// System.out.println(loanTrustVo.getPolicyNumber());
				} else {
					throw new LoanTrustException(
							"Policy number is not in the specified Format");
				}

				if (loanArray[1].matches("(FR)[0-9]{3}")) {
					loanTrustVo.setPanNumber(loanArray[1]);
				} else {
					throw new LoanTrustException(
							"PAN number is not in the specified Format");
				}

				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
				sdf.setLenient(false);
				Date d = sdf.parse(loanArray[2]);
				loanTrustVo.setStartDate(d);

				loanTrustVo.setPeriod(Integer.parseInt(loanArray[3]));

				if (assuredamt > Float.parseFloat(loanArray[4])) {
					loanTrustVo.setAccumulatedPremium(Float
							.parseFloat(loanArray[4]));
					;
				} else {
					throw new LoanTrustException(
							"Accumulated Premium Amount should not be greater than assured amount");
				}

				loanTrustVo
						.setLoanRequestAmount(Float.parseFloat(loanArray[5]));

				loanTrustList.add(loanTrustVo);
			}

			List<LoanTrustVo> elgList = new ArrayList<>();
			List<LoanTrustVo> nonElgList = new ArrayList<>();
			List<LoanTrustVo> dupList = new ArrayList<>();
			List<LoanTrustVo> invList = new ArrayList<>();

			// To Find Duplicate Elements
			Map<Integer, LoanTrustVo> dupMap = new HashMap<Integer, LoanTrustVo>();

			for (LoanTrustVo loanTrustVo : loanTrustList) {

				Calendar c = Calendar.getInstance();
				c.setTime(loanTrustVo.getStartDate());
				c.add(Calendar.MONTH, loanTrustVo.getPeriod());
				Date endDate = c.getTime();
				loanTrustVo.setEndDate(endDate);

				String[] plcysplit = (loanTrustVo.getPolicyNumber()).split("-");
				String plcyTyp = plcysplit[0];
				Integer plcycode = Integer.parseInt(plcysplit[1]);
				Float assuredamt = Float.parseFloat(plcysplit[2]);

				if (dupMap.containsKey(plcycode)) {
					dupList.add(dupMap.get(plcycode));
				} else {
					dupMap.put(plcycode, loanTrustVo);
				}

				Float reqLnAmnt = loanTrustVo.getLoanRequestAmount();
				Float accumpremamt = loanTrustVo.getAccumulatedPremium();

				if (plcyTyp.equals("FST") && reqLnAmnt < (0.6 * accumpremamt)) {
					calculateNetPaymentEligible(loanTrustVo, sanctionDate,
							assuredamt);
					elgList.add(loanTrustVo);
				}

				if (plcyTyp.equals("FST")
						&& (reqLnAmnt > (0.6 * accumpremamt) && reqLnAmnt < (0.7 * assuredamt))) {
					calculateNetPaymentEligible(loanTrustVo, sanctionDate,
							assuredamt);
					elgList.add(loanTrustVo);
				}
				if (plcyTyp.equals("FST")
						&& (reqLnAmnt > (0.6 * accumpremamt) && reqLnAmnt > (0.7 * assuredamt))) {
					calculateNetPaymentNonEligible(loanTrustVo);
					nonElgList.add(loanTrustVo);
				}

				if (plcyTyp.equals("NRM") && reqLnAmnt > (0.4 * accumpremamt)) {
					calculateNetPaymentNonEligible(loanTrustVo);
					nonElgList.add(loanTrustVo);
				}

				if (plcyTyp.equals("NRM") && reqLnAmnt < (0.4 * accumpremamt)) {
					calculateNetPaymentEligible(loanTrustVo, sanctionDate,
							assuredamt);
					elgList.add(loanTrustVo);
				}

				if (reqLnAmnt > assuredamt) {
					invList.add(loanTrustVo);
				}
			}

			innermap1.put("ELG", elgList);
			innermap1.put("NELG", nonElgList);
			innermap2.put("DUP", dupList);
			innermap2.put("INV", invList);

			retMap.put(1, innermap1);
			retMap.put(2, innermap2);

		} catch (FileNotFoundException e) {

			throw new LoanTrustException(e);
		} catch (IOException e) {

			throw new LoanTrustException(e);
		} catch (ParseException e) {

			throw new LoanTrustException(e);
		}

		return retMap;

	}

	private void calculateNetPaymentEligible(LoanTrustVo loanTrustVo,
			String sanctionDate, Float assuredamt) {

		int loanPeriod = 0;
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		Date d = null;
		try {
			d = sdf.parse(sanctionDate);
		} catch (ParseException e) {

			e.printStackTrace();
		}
		Calendar c1 = Calendar.getInstance();
		c1.setTime(d);
		Calendar c2 = Calendar.getInstance();
		c2.setTime(loanTrustVo.getEndDate());
		int years = c2.get(Calendar.YEAR) - c1.get(Calendar.YEAR);
		int months = c2.get(Calendar.MONTH) - c1.get(Calendar.MONTH);
		int datediff = c2.get(Calendar.DATE) - c1.get(Calendar.DATE);
		if (datediff < 0) {
			loanPeriod = ((years * 12) + (months)) - 1;
		} else {
			loanPeriod = (years * 12) + months;
		}
		// System.out.println(loanPeriod);
		Float premAmt = assuredamt / loanTrustVo.getPeriod();
		// System.out.println(premAmt);
		Float interestAmount = (float) ((loanTrustVo.getLoanRequestAmount() * 1.2 * loanPeriod) / 100);
		System.out.println(interestAmount);
		Float increaseinprem = (loanTrustVo.getLoanRequestAmount() + interestAmount)
				/ loanTrustVo.getPeriod();
		Float netPaymentValue = premAmt + increaseinprem;
		loanTrustVo.setNetPremiumAmount(netPaymentValue);
	}

	private void calculateNetPaymentNonEligible(LoanTrustVo loanTrustVo) {

		loanTrustVo.setNetPremiumAmount(loanTrustVo.getAccumulatedPremium());
	}
}

class LoanTrustException extends Exception {

	private static final long serialVersionUID = 1L;

	public LoanTrustException(String msg) {
		super(msg);
	}

	public LoanTrustException(Throwable throwable) {
		super(throwable);
	}

	public LoanTrustException(String msg, Throwable throwable) {
		super(msg, throwable);
	}
}

class LoanTrustVo {
	private String policyNumber;
	private String panNumber;
	private Date startDate;
	private Integer period;

	private float loanRequestAmount;
	private float sumAssured;
	private Date endDate;
	private float accumulatedPremium;
	private float netPremiumAmount;

	@Override
	public String toString() {
		return "LoanTrustVo [policyNumber=" + policyNumber + ", panNumber="
				+ panNumber + ", startDate=" + startDate + ", period=" + period
				+ ", loanRequestAmount=" + loanRequestAmount + ", sumAssured="
				+ sumAssured + ", endDate=" + endDate + ", accumulatedPremium="
				+ accumulatedPremium + ", netPremiumAmount=" + netPremiumAmount
				+ "]";
	}

	public LoanTrustVo() {
		super();

	}

	public LoanTrustVo(String policyNumber, String panNumber, Date startDate,
			Integer period, float loanRequestAmount, float sumAssured,
			Date endDate, float accumulatedPremium, float netPremiumAmount) {
		super();
		this.policyNumber = policyNumber;
		this.panNumber = panNumber;
		this.startDate = startDate;
		this.period = period;
		this.loanRequestAmount = loanRequestAmount;
		this.sumAssured = sumAssured;
		this.endDate = endDate;
		this.accumulatedPremium = accumulatedPremium;
		this.netPremiumAmount = netPremiumAmount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(accumulatedPremium);
		result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
		result = prime * result + Float.floatToIntBits(loanRequestAmount);
		result = prime * result + Float.floatToIntBits(netPremiumAmount);
		result = prime * result
				+ ((panNumber == null) ? 0 : panNumber.hashCode());
		result = prime * result + ((period == null) ? 0 : period.hashCode());
		result = prime * result
				+ ((policyNumber == null) ? 0 : policyNumber.hashCode());
		result = prime * result
				+ ((startDate == null) ? 0 : startDate.hashCode());
		result = prime * result + Float.floatToIntBits(sumAssured);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LoanTrustVo other = (LoanTrustVo) obj;
		if (Float.floatToIntBits(accumulatedPremium) != Float
				.floatToIntBits(other.accumulatedPremium))
			return false;
		if (endDate == null) {
			if (other.endDate != null)
				return false;
		} else if (!endDate.equals(other.endDate))
			return false;
		if (Float.floatToIntBits(loanRequestAmount) != Float
				.floatToIntBits(other.loanRequestAmount))
			return false;
		if (Float.floatToIntBits(netPremiumAmount) != Float
				.floatToIntBits(other.netPremiumAmount))
			return false;
		if (panNumber == null) {
			if (other.panNumber != null)
				return false;
		} else if (!panNumber.equals(other.panNumber))
			return false;
		if (period == null) {
			if (other.period != null)
				return false;
		} else if (!period.equals(other.period))
			return false;
		if (policyNumber == null) {
			if (other.policyNumber != null)
				return false;
		} else if (!policyNumber.equals(other.policyNumber))
			return false;
		if (startDate == null) {
			if (other.startDate != null)
				return false;
		} else if (!startDate.equals(other.startDate))
			return false;
		if (Float.floatToIntBits(sumAssured) != Float
				.floatToIntBits(other.sumAssured))
			return false;
		return true;
	}

	public String getPolicyNumber() {
		return policyNumber;
	}

	public void setPolicyNumber(String policyNumber) {
		this.policyNumber = policyNumber;
	}

	public String getPanNumber() {
		return panNumber;
	}

	public void setPanNumber(String panNumber) {
		this.panNumber = panNumber;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Integer getPeriod() {
		return period;
	}

	public void setPeriod(Integer period) {
		this.period = period;
	}

	public float getLoanRequestAmount() {
		return loanRequestAmount;
	}

	public void setLoanRequestAmount(float loanRequestAmount) {
		this.loanRequestAmount = loanRequestAmount;
	}

	public float getSumAssured() {
		return sumAssured;
	}

	public void setSumAssured(float sumAssured) {
		this.sumAssured = sumAssured;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public float getAccumulatedPremium() {
		return accumulatedPremium;
	}

	public void setAccumulatedPremium(float accumulatedPremium) {
		this.accumulatedPremium = accumulatedPremium;
	}

	public float getNetPremiumAmount() {
		return netPremiumAmount;
	}

	public void setNetPremiumAmount(float netPremiumAmount) {
		this.netPremiumAmount = netPremiumAmount;
	}
}