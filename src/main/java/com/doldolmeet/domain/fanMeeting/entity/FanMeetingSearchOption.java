package com.doldolmeet.domain.fanMeeting.entity;

public enum FanMeetingSearchOption {
    ALL("all"), CLOSED("closed"), OPENED("opened"), PROGRESS("progress");

    String value;
    FanMeetingSearchOption(String value) {
        this.value = value;
    }
    public String value() {
        return value;
    }
}
