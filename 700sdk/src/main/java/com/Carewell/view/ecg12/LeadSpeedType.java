package com.Carewell.view.ecg12;

public enum LeadSpeedType {
    FORMFEED_5 ("5mm/s",5f),
    FORMFEED_6_P_25 ("6.25mm/s",6.25f),
    FORMFEED_10 ("10mm/s",10f),
    FORMFEED_12_P_5 ("12.5mm/s",12.5f),
    FORMFEED_25 ("25mm/s",25f),
    FORMFEED_50 ("50mm/s",50f);

    private String name;
    private Float value;

    LeadSpeedType(String name, Float value){
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public static LeadSpeedType getLeadEnumByValue(Object value){
        LeadSpeedType type = LeadSpeedType.FORMFEED_25;
        for(LeadSpeedType item : LeadSpeedType.values()){
            if(item.getValue() == value){
                type = item;
                break;
            }
        }
        return type;
    }
}