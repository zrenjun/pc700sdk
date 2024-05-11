package com.Carewell.ecg700.entity;

public class EcgSettingConfigEnum {

    public enum LeadType {
        //value值不能修改，传统算法分析时用
        LEAD_9( 8),
        /**
         * 9导联
         **/
        LEAD_12(1),
        /**
         * 12导联
         **/
        LEAD_15_STANDARD_RIGHT( 6),
        /**
         * 15导联 标准+右
         **/
        LEAD_15_STANDARD_AFTER(5),
        /**
         * 15导联 标准+后
         **/
        LEAD_15_CHILD( 9),
        /**
         * 15导联 儿童模式
         **/
        LEAD_18( 7),
        /**
         * 18导联
         **/
        LEAD_6( 1);

        private int value;

        LeadType( int value) {
            this.value = value;
        }


        public int getValue() {
            return value;
        }

    }
}
