package io.github.kuvandikov.scan_card_nfc.iso7816emv;


import io.github.kuvandikov.scan_card_nfc.enums.TagTypeEnum;
import io.github.kuvandikov.scan_card_nfc.enums.TagValueTypeEnum;

public interface ITag {

	enum Class {
		UNIVERSAL, APPLICATION, CONTEXT_SPECIFIC, PRIVATE
	}

	boolean isConstructed();

	byte[] getTagBytes();

	String getName();

	String getDescription();

	TagTypeEnum getType();

	TagValueTypeEnum getTagValueType();

	Class getTagClass();

	int getNumTagBytes();

}
