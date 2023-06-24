package com.edata;

import org.assertj.core.util.Lists;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author yugt 2023/6/23
 */
public class FeatureTest {

    public static void main(String[] args) {
        List<String> nums = Lists.newArrayList("a","b","c");

        String s = nums.remove(0);
        System.out.println(s+"---"+nums.size());
        s = nums.remove(0);
        System.out.println(s+"---"+nums);

        int sum = List.of(1,2,3).stream()
                .reduce(0,Integer::sum);
        System.out.println("sum: "+sum);
        record User(String name,Integer age){}
        User user = new User("bike",35);

        Date date1 = new Date();
        LocalDate localDate1 = LocalDate.now();
        LocalDate localDate = LocalDate.parse("2023-06-26");
        System.out.println(localDate);
        System.out.println(localDate1);
        Date date2 = new Date();
        Calendar calendar = Calendar.getInstance();
        LocalDate localDate2 = LocalDate.now();
        System.out.println(localDate2);
        System.out.println(localDate1.equals(localDate2));
    }

}
