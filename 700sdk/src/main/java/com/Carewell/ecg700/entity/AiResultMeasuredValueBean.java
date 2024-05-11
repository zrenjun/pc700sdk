
package com.Carewell.ecg700.entity;

public class AiResultMeasuredValueBean {
    private String QRStype;//8个字节
    private short Pa1;//use it 都用1
    private short Pa2;
    private short Qa;
    private short Ra1;
    private short Ra2;
    private short Sa1;
    private short Sa2;
    private short Ta1;
    private short Ta2;
    private short Ua;

    private short Pd;
    private short Qd;
    private short Rd1;
    private short Rd2;
    private short Sd1;
    private short Sd2;
    private short Td;
    private short Ud;

    private short PR;
    private short QT;
    private short QRS;
    private short VAT;

    private short STj;
    private short ST1;
    private short ST2;
    private short ST3;
    private short ST20;
    private short ST40;
    private short ST60;
    private short ST80;
    private short nRNotch;	//R波缺口(0,1,2,3,4:无，上升，下降，两边都有,切迹)
    private short Delta;

    public String getQRStype() {
        return QRStype;
    }

    public void setQRStype(String QRStype) {
        this.QRStype = QRStype;
    }

    public short getPa1() {
        return Pa1;
    }

    public void setPa1(short pa1) {
        Pa1 = pa1;
    }

    public short getPa2() {
        return Pa2;
    }

    public void setPa2(short pa2) {
        Pa2 = pa2;
    }

    public short getQa() {
        return Qa;
    }

    public void setQa(short qa) {
        Qa = qa;
    }

    public short getRa1() {
        return Ra1;
    }

    public void setRa1(short ra1) {
        Ra1 = ra1;
    }

    public short getRa2() {
        return Ra2;
    }

    public void setRa2(short ra2) {
        Ra2 = ra2;
    }

    public short getSa1() {
        return Sa1;
    }

    public void setSa1(short sa1) {
        Sa1 = sa1;
    }

    public short getSa2() {
        return Sa2;
    }

    public void setSa2(short sa2) {
        Sa2 = sa2;
    }

    public short getTa1() {
        return Ta1;
    }

    public void setTa1(short ta1) {
        Ta1 = ta1;
    }

    public short getTa2() {
        return Ta2;
    }

    public void setTa2(short ta2) {
        Ta2 = ta2;
    }

    public short getUa() {
        return Ua;
    }

    public void setUa(short ua) {
        Ua = ua;
    }

    public short getPd() {
        return Pd;
    }

    public void setPd(short pd) {
        Pd = pd;
    }

    public short getQd() {
        return Qd;
    }

    public void setQd(short qd) {
        Qd = qd;
    }

    public short getRd1() {
        return Rd1;
    }

    public void setRd1(short rd1) {
        Rd1 = rd1;
    }

    public short getRd2() {
        return Rd2;
    }

    public void setRd2(short rd2) {
        Rd2 = rd2;
    }

    public short getSd1() {
        return Sd1;
    }

    public void setSd1(short sd1) {
        Sd1 = sd1;
    }

    public short getSd2() {
        return Sd2;
    }

    public void setSd2(short sd2) {
        Sd2 = sd2;
    }

    public short getTd() {
        return Td;
    }

    public void setTd(short td) {
        Td = td;
    }

    public short getUd() {
        return Ud;
    }

    public void setUd(short ud) {
        Ud = ud;
    }

    public short getPR() {
        return PR;
    }

    public void setPR(short PR) {
        this.PR = PR;
    }

    public short getQT() {
        return QT;
    }

    public void setQT(short QT) {
        this.QT = QT;
    }

    public short getQRS() {
        return QRS;
    }

    public void setQRS(short QRS) {
        this.QRS = QRS;
    }

    public short getVAT() {
        return VAT;
    }

    public void setVAT(short VAT) {
        this.VAT = VAT;
    }

    public short getSTj() {
        return STj;
    }

    public void setSTj(short STj) {
        this.STj = STj;
    }

    public short getST1() {
        return ST1;
    }

    public void setST1(short ST1) {
        this.ST1 = ST1;
    }

    public short getST2() {
        return ST2;
    }

    public void setST2(short ST2) {
        this.ST2 = ST2;
    }

    public short getST3() {
        return ST3;
    }

    public void setST3(short ST3) {
        this.ST3 = ST3;
    }

    public short getST20() {
        return ST20;
    }

    public void setST20(short ST20) {
        this.ST20 = ST20;
    }

    public short getST40() {
        return ST40;
    }

    public void setST40(short ST40) {
        this.ST40 = ST40;
    }

    public short getST60() {
        return ST60;
    }

    public void setST60(short ST60) {
        this.ST60 = ST60;
    }

    public short getST80() {
        return ST80;
    }

    public void setST80(short ST80) {
        this.ST80 = ST80;
    }

    public short getnRNotch() {
        return nRNotch;
    }

    public void setnRNotch(short nRNotch) {
        this.nRNotch = nRNotch;
    }

    public short getDelta() {
        return Delta;
    }

    public void setDelta(short delta) {
        Delta = delta;
    }
}
