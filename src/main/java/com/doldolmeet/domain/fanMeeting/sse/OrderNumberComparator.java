package com.doldolmeet.domain.fanMeeting.sse;

import java.util.Comparator;

public class OrderNumberComparator implements Comparator<UserNameAndOrderNumber> {

    // orderNumber 기준 오름차순 정렬
    @Override
    public int compare(UserNameAndOrderNumber o1, UserNameAndOrderNumber o2) {
        if(o1.getOrderNumber() < o2.getOrderNumber()) return -1;
        else if(o1.getOrderNumber().equals(o2.getOrderNumber())) return 0;
        else return 1;
    }
}