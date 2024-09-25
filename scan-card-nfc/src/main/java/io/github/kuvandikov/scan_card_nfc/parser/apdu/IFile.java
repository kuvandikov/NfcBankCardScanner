package io.github.kuvandikov.scan_card_nfc.parser.apdu;

import java.util.List;

import io.github.kuvandikov.scan_card_nfc.iso7816emv.TagAndLength;

/**
 * Interface for File to parse
 */
public interface IFile {

	/**
	 * Method to parse byte data
	 * 
	 * @param pData
	 *            byte to parse
	 * @param pList
	 *            Tag and length
	 */
	void parse(final byte[] pData, final List<TagAndLength> pList);

}
