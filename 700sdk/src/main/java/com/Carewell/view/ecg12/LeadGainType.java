package com.Carewell.view.ecg12;

public enum LeadGainType {
    GAIN_2_P_5 ("2.5mm/mV","2.5"),
    GAIN_5 ("5mm/mV","5"),
    GAIN_10 ("10mm/mV","10"),
    GAIN_20 ("20mm/mV","20"),
    GAIN_40 ("40mm/mV","40");

    private String name;
    private Object value;

    LeadGainType(String name, Object value){
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public static LeadGainType getLeadEnumByValue(Object value){
        LeadGainType type = LeadGainType.GAIN_10;
        for(LeadGainType item : LeadGainType.values()){
            if(item.getValue() == value){
                type = item;
                break;
            }
        }
        return type;
    }
}
