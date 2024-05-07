package com.Carewell.view.ecg12;

public enum RecordOrderType {
    /**
     * 顺序记录
     */
    ORDER_INORDER ("",""),
    /**
     * 同步记录
     */
    ORDER_SYNC("","");

    private String name;
    private Object value;

    RecordOrderType(String name, Object value){
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
}
