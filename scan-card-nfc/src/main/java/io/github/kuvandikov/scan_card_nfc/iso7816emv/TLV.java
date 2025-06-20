package io.github.kuvandikov.scan_card_nfc.iso7816emv;

/**
 * Tag and length value
 * 
 */
public class TLV {

	/**
	 * Tag
	 */
	private ITag tag;
	/**
	 * length in bytes
	 */
	private byte[] rawEncodedLengthBytes;
	/**
	 * Value in bytes
	 */
	private byte[] valueBytes;
	/**
	 * Tag length
	 */
	private int length;

	/**
	 * 
	 * @param tag
	 * @param length
	 *            contains the number of value bytes (parsed from the rawEncodedLengthBytes)
	 * @param rawLengthBytes
	 *            the raw encoded length bytes
	 * @param valueBytes
	 */
	public TLV(final ITag tag, final int length, final byte[] rawEncodedLengthBytes, final byte[] valueBytes) {
		if (valueBytes == null || length != valueBytes.length) {
			// Assert
			throw new IllegalArgumentException("length != bytes.length");
		}
		this.tag = tag;
		this.rawEncodedLengthBytes = rawEncodedLengthBytes;
		this.valueBytes = valueBytes;
		this.length = length;
	}

	/**
	 * Method used to get the field tag
	 * 
	 * @return the tag
	 */
	public ITag getTag() {
		return tag;
	}

	/**
	 * Setter for the field tag
	 * 
	 * @param tag
	 *            the tag to set
	 */
	public void setTag(final ITag tag) {
		this.tag = tag;
	}

	/**
	 * Method used to get the field rawEncodedLengthBytes
	 * 
	 * @return the rawEncodedLengthBytes
	 */
	public byte[] getRawEncodedLengthBytes() {
		return rawEncodedLengthBytes;
	}

	/**
	 * Setter for the field rawEncodedLengthBytes
	 * 
	 * @param rawEncodedLengthBytes
	 *            the rawEncodedLengthBytes to set
	 */
	public void setRawEncodedLengthBytes(final byte[] rawEncodedLengthBytes) {
		this.rawEncodedLengthBytes = rawEncodedLengthBytes;
	}

	/**
	 * Method used to get the field valueBytes
	 * 
	 * @return the valueBytes
	 */
	public byte[] getValueBytes() {
		return valueBytes;
	}

	/**
	 * Setter for the field valueBytes
	 * 
	 * @param valueBytes
	 *            the valueBytes to set
	 */
	public void setValueBytes(final byte[] valueBytes) {
		this.valueBytes = valueBytes;
	}

	/**
	 * Method used to get the field length
	 * 
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Setter for the field length
	 * 
	 * @param length
	 *            the length to set
	 */
	public void setLength(final int length) {
		this.length = length;
	}

	/**
	 * Get tag bytes
	 * 
	 * @return tag bytes
	 */
	public byte[] getTagBytes() {
		return tag.getTagBytes();
	}

}
