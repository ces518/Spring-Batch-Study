package me.june.chapter07;

import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.item.file.transform.DefaultFieldSetFactory;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.FieldSetFactory;
import org.springframework.batch.item.file.transform.LineTokenizer;

public class CustomerFileLineTokenizer implements LineTokenizer {

    private static final String DELIMITER = ",";
    private static final String[] NAMES = new String[]{
        "firstName", "middleInitial", "lastName", "address", "city", "state", "zipCode"
    };
    private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

    @Override
    public FieldSet tokenize(String record) {
        String[] fields = record.split(DELIMITER);
        List<String> paredFields = new ArrayList<>();

        for (int i = 0; i < fields.length; i++) {
            if (i == 4) {
                // 3, 4번 컬럼을 하나로 합침
                paredFields.set(i - 1, paredFields.get(i - 1) + " " + fields[i]);
            } else {
                paredFields.add(fields[i]);
            }
        }

        return fieldSetFactory.create(paredFields.toArray(new String[0]), NAMES);
    }
}
