package io.github.kuvandikov.scan_card_nfc.parser.apdu.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import fr.devnied.bitlib.BitUtils;
import io.github.kuvandikov.scan_card_nfc.model.enums.IKeyEnum;
import io.github.kuvandikov.scan_card_nfc.parser.apdu.annotation.AnnotationData;
import io.github.kuvandikov.scan_card_nfc.utils.EnumUtils;


/**
 * Factory to parse data
 */
public final class DataFactory {

	/**
	 * Logger of this class
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(DataFactory.class.getName());

	/**
	 * Constant for EN1545-1 (Date format)
	 */
	public static final int BCD_DATE = 1;

	/**
	 * Half byte size
	 */
	public static final int HALF_BYTE_SIZE = 4;

	/**
	 * BCD format
	 */
	public static final String BCD_FORMAT = "BCD_Format";

	/**
	 * Method to get a date from the bytes array
	 * 
	 * @param pAnnotation
	 *            annotation data
	 * @param pBit
	 *            table bytes
	 * @return The date read of null
	 */
	private static Date getDate(final AnnotationData pAnnotation, final BitUtils pBit) {
		Date date = null;
		if (pAnnotation.getDateStandard() == BCD_DATE) {
			date = pBit.getNextDate(pAnnotation.getSize(), pAnnotation.getFormat(), true);
		} else {
			date = pBit.getNextDate(pAnnotation.getSize(), pAnnotation.getFormat());
		}
		return date;
	}

	/**
	 * This method is used to get an integer
	 * 
	 * @param pAnnotation
	 *            annotation
	 * @param pObject
	 *            the object to set
	 * @param pBit
	 *            bit array
	 */
	private static int getInteger(final AnnotationData pAnnotation, final BitUtils pBit) {
		return pBit.getNextInteger(pAnnotation.getSize());
	}

	/**
	 * Method to read and object from the bytes tab
	 * 
	 * @param pAnnotation
	 *            all information data
	 * @param pBit
	 *            bytes tab
	 * @return an object
	 */
	public static Object getObject(final AnnotationData pAnnotation, final BitUtils pBit) {
		Object obj = null;
		Class<?> clazz = pAnnotation.getField().getType();

		if (clazz.equals(Integer.class)) {
			obj = getInteger(pAnnotation, pBit);
		} else if (clazz.equals(Float.class)) {
			obj = getFloat(pAnnotation, pBit);
		} else if (clazz.equals(String.class)) {
			obj = getString(pAnnotation, pBit);
		} else if (clazz.equals(Date.class)) {
			obj = getDate(pAnnotation, pBit);
		} else if (clazz.equals(Boolean.class)) {
			obj = pBit.getNextBoolean();
		} else if (clazz.isEnum()) {
			obj = getEnum(pAnnotation, pBit);
		}
		return obj;
	}

	/**
	 * Method use to get float
	 * 
	 * @param pAnnotation
	 *            annotation
	 * @param pBit
	 *            bit utils
	 * @return
	 */
	private static Float getFloat(final AnnotationData pAnnotation, final BitUtils pBit) {
		Float ret = null;

		if (BCD_FORMAT.equals(pAnnotation.getFormat())) {
			ret = Float.parseFloat(pBit.getNextHexaString(pAnnotation.getSize()));
		} else {
			ret = (float) getInteger(pAnnotation, pBit);
		}

		return ret;
	}

	/**
	 * This method is used to get an enum with his key
	 * 
	 * @param pAnnotation
	 *            annotation
	 * @param pBit
	 *            bit array
	 */
	@SuppressWarnings("unchecked")
	private static IKeyEnum getEnum(final AnnotationData pAnnotation, final BitUtils pBit) {
		int val = 0;
		try {
			val = Integer.parseInt(pBit.getNextHexaString(pAnnotation.getSize()), pAnnotation.isReadHexa() ? 16 : 10);
		} catch (NumberFormatException nfe) {
			// do nothing
		}
		return EnumUtils.getValue(val, (Class<? extends IKeyEnum>) pAnnotation.getField().getType());
	}

	/**
	 * This method get a string (Hexa or ASCII) from a bit table
	 * 
	 * @param pAnnotation
	 *            annotation data
	 * @param pBit
	 *            bit table
	 * @return A string
	 */
	private static String getString(final AnnotationData pAnnotation, final BitUtils pBit) {
		String obj = null;

		if (pAnnotation.isReadHexa()) {
			obj = pBit.getNextHexaString(pAnnotation.getSize());
		} else {
			obj = pBit.getNextString(pAnnotation.getSize()).trim();
		}

		return obj;
	}

	/**
	 * Private constructor
	 */
	private DataFactory() {
	}
}
