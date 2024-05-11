package com.Carewell.ecg700.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.io.Serializable;
import java.util.Objects;

/**
 * 患者信息
 */
public class PatientInfoBean implements Parcelable, Serializable {

    //档案
    private String patientNumber;//编号
    private String lastName;//姓
    private String middleName;//中间名;
    private String firstName;//名
    private String archivesName;//名字，已经拼接好。缓存了。由于算法获取的名字，性别，年龄，字段固定，所以名字先用以前的.  数据库表没有存
    private String archivesSex;//sex; 性别 0 男 1 女 2 未知
    private String archivesAge;//age; 年龄
    private String birthdate;//出生日期
    private String height;//身高
    private String weight;//体重
    private String bloodPressure;//血压
    private int race;//种族
    private String nowUseMedicine;
    private String historyDescribed;
    private int patientFrom;
    private String idNumber;
    private String bedNo;
    private String outPatientNo;
    private String inPatientNo;
    private String physicalNumber;
    private String applyDepartment;
    private String applyDoctor;
    private String checkDepartment;
    private String checkTechnician;
    private EcgSettingConfigEnum.LeadType checkLeadType;//检查导联类型
    private String otherContent;
    private boolean pacemaker = false;//是否有起搏器
    private String checkNo;

    private String appointmentId;//预约id 主要方便预约列表采集数据和病例管理列表关联
    private String patientId ;//患者id 云上传返回的
    private PatientSettingConfigEnum.HeightWeightUnit hwUnit; //身高、体重单位，该值只会影响展示效果，height/weight 存的是 cm/kg
    private PatientSettingConfigEnum.BloodPressureUnit bpUnit; //血压单位，该值只会影响展示效果，bloodPressure存的是 mmHg

    private int leadoffstate;//导联脱落情况
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.patientNumber);
        dest.writeString(this.lastName);
        dest.writeString(this.middleName);
        dest.writeString(this.firstName);

        StringBuilder sb = new StringBuilder();
        if(!TextUtils.isEmpty(this.lastName)){
            sb.append(this.lastName);
        }

        if(!TextUtils.isEmpty(this.middleName)){
            sb.append(this.middleName);
        }
        if(!TextUtils.isEmpty(sb.toString())){
            this.archivesName = sb.toString();
        }

        dest.writeString(this.archivesName);

        dest.writeString(this.archivesSex);
        dest.writeString(this.archivesAge);
        dest.writeString(this.birthdate);
        dest.writeString(this.height);
        dest.writeString(this.weight);
        dest.writeString(this.bloodPressure);
        dest.writeInt(this.race);
        dest.writeString(this.nowUseMedicine);
        dest.writeString(this.historyDescribed);
        dest.writeInt(this.patientFrom);
        dest.writeString(this.idNumber);
        dest.writeString(this.bedNo);
        dest.writeString(this.outPatientNo);
        dest.writeString(this.inPatientNo);
        dest.writeString(this.physicalNumber);
        dest.writeString(this.applyDepartment);
        dest.writeString(this.applyDoctor);
        dest.writeString(this.checkDepartment);
        dest.writeString(this.checkTechnician);
        dest.writeInt(this.checkLeadType == null ? -1 : this.checkLeadType.ordinal());
        dest.writeString(this.otherContent);
        dest.writeByte(this.pacemaker ? (byte) 1 : (byte) 0);
        dest.writeString(this.checkNo);
        dest.writeString(this.appointmentId);
        dest.writeString(this.patientId);
        dest.writeInt(this.hwUnit==null?-1:this.hwUnit.ordinal());
        dest.writeInt(this.bpUnit==null?-1:this.bpUnit.ordinal());
        dest.writeInt(this.leadoffstate);
    }

    public PatientInfoBean() {
    }

    protected PatientInfoBean(Parcel in) {
        this.leadoffstate = in.readInt();
        this.patientNumber = in.readString();
        this.lastName = in.readString();
        this.middleName = in.readString();
        this.firstName = in.readString();
        this.archivesName = in.readString();
        this.archivesSex = in.readString();
        this.archivesAge = in.readString();
        this.birthdate = in.readString();
        this.height = in.readString();
        this.weight = in.readString();
        this.bloodPressure = in.readString();
        this.race = in.readInt();
        this.nowUseMedicine = in.readString();
        this.historyDescribed = in.readString();
        this.patientFrom = in.readInt();
        this.idNumber = in.readString();
        this.bedNo = in.readString();
        this.outPatientNo = in.readString();
        this.inPatientNo = in.readString();
        this.physicalNumber = in.readString();
        this.applyDepartment = in.readString();
        this.applyDoctor = in.readString();
        this.checkDepartment = in.readString();
        this.checkTechnician = in.readString();
        int tmpCheckCaseLeadType = in.readInt();
        this.checkLeadType = tmpCheckCaseLeadType == -1 ? null : EcgSettingConfigEnum.LeadType.values()[tmpCheckCaseLeadType];
        this.otherContent = in.readString();
        this.pacemaker = in.readByte() != 0;
        int tmpIdMakeType = in.readInt();
        this.checkNo=in.readString();
        this.appointmentId = in.readString();
        this.patientId=in.readString();
        int unit = in.readInt();
        this.hwUnit = unit==-1?null: PatientSettingConfigEnum.HeightWeightUnit.values()[unit];
        unit = in.readInt();
        this.bpUnit = unit==-1?null: PatientSettingConfigEnum.BloodPressureUnit.values()[unit];

        //处理空字符串问题,不处理，导致外面null指令问题等
        this.patientNumber = this.patientNumber == null ? "" : this.patientNumber;
        this.lastName = this.lastName == null ? "" : this.lastName;
        this.middleName = this.middleName == null ? "" : this.middleName;
        this.firstName = this.firstName == null ? "" : this.firstName;
        this.archivesName = this.archivesName == null ? "" : this.archivesName;
        this.archivesSex = this.archivesSex == null ? "" : this.archivesSex;
        this.archivesAge = this.archivesAge == null ? "" : this.archivesAge;
        this.birthdate = this.birthdate == null ? "" : this.birthdate;
        this.height = this.height == null ? "" : this.height;
        this.weight = this.weight == null ? "" : this.weight;
        this.bloodPressure = this.bloodPressure == null ? "" : this.bloodPressure;
        this.nowUseMedicine = this.nowUseMedicine == null ? "" : this.nowUseMedicine;
        this.historyDescribed = this.historyDescribed == null ? "" : this.historyDescribed;
        this.idNumber = this.idNumber == null ? "" : this.idNumber;
        this.bedNo = this.bedNo == null ? "" : this.bedNo;
        this.outPatientNo = this.outPatientNo == null ? "" : this.outPatientNo;
        this.inPatientNo = this.inPatientNo == null ? "" : this.inPatientNo;
        this.physicalNumber = this.physicalNumber == null ? "" : this.physicalNumber;
        this.applyDepartment = this.applyDepartment == null ? "" : this.applyDepartment;
        this.applyDoctor = this.applyDoctor == null ? "" : this.applyDoctor;
        this.checkDepartment = this.checkDepartment == null ? "" : this.checkDepartment;
        this.checkTechnician = this.checkTechnician == null ? "" : this.checkTechnician;
        this.otherContent = this.otherContent == null ? "" : this.otherContent;
        this.checkNo = this.checkNo == null ? "" : this.checkNo;
        this.appointmentId = this.appointmentId == null ? "" : this.appointmentId;
        this.patientId = this.patientId == null ? "" : this.patientId;
    }

    public static final Creator<PatientInfoBean> CREATOR = new Creator<PatientInfoBean>() {
        @Override
        public PatientInfoBean createFromParcel(Parcel source) {
            return new PatientInfoBean(source);
        }

        @Override
        public PatientInfoBean[] newArray(int size) {
            return new PatientInfoBean[size];
        }
    };

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public String getLastName() {
        if (TextUtils.isEmpty(lastName))
            return "";
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        if (TextUtils.isEmpty(middleName))
            return "";
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getFirstName() {
        if (TextUtils.isEmpty(firstName))
            return "";
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSex() {
        return archivesSex;
    }

    public void setSex(String sex) {
        this.archivesSex = sex;
    }

    public String getAge() {
        return archivesAge;
    }

    public void setAge(String age) {
        this.archivesAge = age;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(String bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public int getRace() {
        return race;
    }

    public void setRace(int race) {
        this.race = race;
    }

    public String getNowUseMedicine() {
        return nowUseMedicine;
    }

    public void setNowUseMedicine(String nowUseMedicine) {
        this.nowUseMedicine = nowUseMedicine;
    }

    public String getHistoryDescribed() {
        return historyDescribed;
    }

    public void setHistoryDescribed(String historyDescribed) {
        this.historyDescribed = historyDescribed;
    }

    public int getPatientFrom() {
        return patientFrom;
    }

    public void setPatientFrom(int patientFrom) {
        this.patientFrom = patientFrom;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getBedNo() {
        return bedNo;
    }

    public void setBedNo(String bedNo) {
        this.bedNo = bedNo;
    }

    public String getOutPatientNo() {
        return outPatientNo;
    }

    public void setOutPatientNo(String outPatientNo) {
        this.outPatientNo = outPatientNo;
    }

    public String getInPatientNo() {
        return inPatientNo;
    }

    public void setInPatientNo(String inPatientNo) {
        this.inPatientNo = inPatientNo;
    }

    public String getPhysicalNumber() {
        return physicalNumber;
    }

    public void setPhysicalNumber(String physicalNumber) {
        this.physicalNumber = physicalNumber;
    }

    public String getApplyDepartment() {
        return applyDepartment;
    }

    public void setApplyDepartment(String applyDepartment) {
        this.applyDepartment = applyDepartment;
    }

    public String getApplyDoctor() {
        return applyDoctor;
    }

    public void setApplyDoctor(String applyDoctor) {
        this.applyDoctor = applyDoctor;
    }

    public String getCheckDepartment() {
        return checkDepartment;
    }

    public void setCheckDepartment(String checkDepartment) {
        this.checkDepartment = checkDepartment;
    }

    public String getCheckTechnician() {
        return checkTechnician;
    }

    public void setCheckTechnician(String checkTechnician) {
        this.checkTechnician = checkTechnician;
    }

    public EcgSettingConfigEnum.LeadType getCheckLeadType() {
        return checkLeadType;
    }

    public void setCheckLeadType(EcgSettingConfigEnum.LeadType checkLeadType) {
        this.checkLeadType = checkLeadType;
    }

    public String getOtherContent() {
        return otherContent;
    }

    public void setOtherContent(String otherContent) {
        this.otherContent = otherContent;
    }

    public boolean isPacemaker() {
        return pacemaker;
    }

    public void setPacemaker(boolean pacemaker) {
        this.pacemaker = pacemaker;
    }

    public String getArchivesName() {
        return archivesName;
    }

    public void setArchivesName(String archivesName) {
        this.archivesName = archivesName;
    }


    public String getCheckNo() {
        return checkNo;
    }

    public void setCheckNo(String checkNo) {
        this.checkNo = checkNo;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public PatientSettingConfigEnum.HeightWeightUnit getHwUnit() {
        return hwUnit;
    }

    public void setHwUnit(PatientSettingConfigEnum.HeightWeightUnit hwUnit) {
        this.hwUnit = hwUnit;
    }

    public PatientSettingConfigEnum.BloodPressureUnit getBpUnit() {
        return bpUnit;
    }

    public void setBpUnit(PatientSettingConfigEnum.BloodPressureUnit bpUnit) {
        this.bpUnit = bpUnit;
    }

    public int getLeadoffstate() {
        return leadoffstate;
    }

    public void setLeadoffstate(int leadoffstate) {
        this.leadoffstate = leadoffstate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatientInfoBean that = (PatientInfoBean) o;
        return race == that.race &&
                patientFrom == that.patientFrom &&
                pacemaker == that.pacemaker &&
                leadoffstate == that.leadoffstate &&
                Objects.equals(patientNumber, that.patientNumber) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(middleName, that.middleName) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(archivesName, that.archivesName) &&
                Objects.equals(archivesSex, that.archivesSex) &&
                Objects.equals(archivesAge, that.archivesAge) &&
                Objects.equals(birthdate, that.birthdate) &&
                Objects.equals(height, that.height) &&
                Objects.equals(weight, that.weight) &&
                Objects.equals(bloodPressure, that.bloodPressure) &&
                Objects.equals(nowUseMedicine, that.nowUseMedicine) &&
                Objects.equals(historyDescribed, that.historyDescribed) &&
                Objects.equals(idNumber, that.idNumber) &&
                Objects.equals(bedNo, that.bedNo) &&
                Objects.equals(outPatientNo, that.outPatientNo) &&
                Objects.equals(inPatientNo, that.inPatientNo) &&
                Objects.equals(physicalNumber, that.physicalNumber) &&
                Objects.equals(applyDepartment, that.applyDepartment) &&
                Objects.equals(applyDoctor, that.applyDoctor) &&
                Objects.equals(checkDepartment, that.checkDepartment) &&
                Objects.equals(checkTechnician, that.checkTechnician) &&
                checkLeadType == that.checkLeadType &&
                Objects.equals(otherContent, that.otherContent) &&
                Objects.equals(checkNo, that.checkNo) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientNumber, lastName, middleName, firstName, archivesName, archivesSex, archivesAge,  birthdate, height, weight, bloodPressure, race, nowUseMedicine, historyDescribed, patientFrom, idNumber, bedNo, outPatientNo, inPatientNo, physicalNumber, applyDepartment, applyDoctor, checkDepartment, checkTechnician, checkLeadType,  otherContent, pacemaker,checkNo, leadoffstate);
    }
}

