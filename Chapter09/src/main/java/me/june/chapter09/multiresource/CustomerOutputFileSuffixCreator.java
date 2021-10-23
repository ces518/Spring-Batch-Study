package me.june.chapter09.multiresource;

import org.springframework.batch.item.file.ResourceSuffixCreator;

public class CustomerOutputFileSuffixCreator implements ResourceSuffixCreator {

    @Override
    public String getSuffix(int index) {
        return index + ".xml";
    }
}
