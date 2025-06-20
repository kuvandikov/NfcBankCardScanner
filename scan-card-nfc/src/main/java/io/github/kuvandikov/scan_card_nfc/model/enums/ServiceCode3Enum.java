package io.github.kuvandikov.scan_card_nfc.model.enums;

/**
 * Service code, position 2 values.
 */
public enum ServiceCode3Enum implements IKeyEnum {

	NO_RESTRICTION_PIN_REQUIRED(0, "No restrictions", "PIN required"),
	NO_RESTRICTION(1, "No restrictions", "None"),
	GOODS_SERVICES(2, "Goods and services only", "None"),
	ATM_ONLY(3, "ATM only", "PIN required"),
	CASH_ONLY(4, "Cash only", "None"),
	GOODS_SERVICES_PIN_REQUIRED(5, "Goods and services only", "PIN required"),
	NO_RESTRICTION_PIN_IF_PED(6, "No restrictions", "Prompt for PIN if PED present"),
	GOODS_SERVICES_PIN_IF_PED(7, "Goods and services only", "Prompt for PIN if PED present"), ;

	private final int value;
	private final String allowedServices;
	private final String pinRequirements;

	private ServiceCode3Enum(final int value, final String allowedServices, final String pinRequirements) {
		this.value = value;
		this.allowedServices = allowedServices;
		this.pinRequirements = pinRequirements;
	}

	/**
	 * Gets the allowed services.
	 * 
	 * @return Allowed services.
	 */
	public String getAllowedServices() {
		return allowedServices;
	}

	/**
	 * Gets the the PIN requirements.
	 * 
	 * @return PIN requirements.
	 */
	public String getPinRequirements() {
		return pinRequirements;
	}

	@Override
	public int getKey() {
		return value;
	}

}