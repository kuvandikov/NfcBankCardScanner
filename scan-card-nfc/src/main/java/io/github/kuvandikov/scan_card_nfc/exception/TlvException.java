package io.github.kuvandikov.scan_card_nfc.exception;

/**
 * Exception during TLV reading
 * 
 */
public class TlvException extends RuntimeException {

	/**
	 * Constructor using field
	 * 
	 * @param pCause
	 *            cause
	 */
	public TlvException(final String pCause) {
		super(pCause);
	}

}
