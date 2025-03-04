package com.aedsiii.puc.model;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

public class Job {
    short job_id;
    String experience;
    String qualification;
    String salary_range;
    String location;
    String country;
    float latitude;
    float longitude;
    String work_type;
    int company_size;
    Instant job_posting_date;
    String preference;
    String contact_person;
    String contact;
    String job_title;
    String role;
    String job_portal;
    String job_description;
    List<String> benefits;
    List<String> skills;
    List<String> responsibilities;
    String company;
    String company_profile;
    public Job(){
        this.job_id = -1;
        this.latitude = this.longitude = 0;
        this.experience = this.qualification = this.salary_range = this.location = this.country = this.work_type = this.contact = this.job_portal =
        this.preference = this.contact_person = this.job_title = this.role = this.job_description = this.company = this.company_profile = null;
        this.skills = this.responsibilities = null;
        this.job_posting_date = null;
        this.benefits = null;
    }
    public Job(short job_id, String experience, String qualification, String salary_range, String location, String country, float latitude, float longitude, String work_type, int company_size, Instant job_posting_date, 
        String preference, String contact_person, String contact, String job_title, String role, String job_portal, String job_description, List<String> benefits, List<String> skills, List<String> responsibilities, 
        String company, String company_profile) {
        this.job_id = job_id;
        this.experience = experience;
        this.qualification = qualification;
        this.salary_range = salary_range;
        this.location = location;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.work_type = work_type;
        this.company_size = company_size;
        this.job_posting_date = job_posting_date;
        this.preference = preference;
        this.contact_person = contact_person;
        this.contact = contact;
        this.job_title = job_title;
        this.role = role;
        this.job_portal = job_portal;
        this.job_description = job_description;
        this.benefits = benefits;
        this.skills = skills;
        this.responsibilities = responsibilities;
        this.company = company;
        this.company_profile = company_profile;
    }
    public short getJob_id() {
        return job_id;
    }

    public void setJob_id(short job_id) {
        this.job_id = job_id;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getSalary_range() {
        return salary_range;
    }

    public void setSalary_range(String salary_range) {
        this.salary_range = salary_range;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public String getWork_type() {
        return work_type;
    }

    public void setWork_type(String work_type) {
        this.work_type = work_type;
    }

    public int getCompany_size() {
        return company_size;
    }

    public void setCompany_size(int company_size) {
        this.company_size = company_size;
    }

    public Instant getJob_posting_date() {
        return job_posting_date;
    }

    public void setJob_posting_date(Instant job_posting_date) {
        this.job_posting_date = job_posting_date;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public String getContact_person() {
        return contact_person;
    }

    public void setContact_person(String contact_person) {
        this.contact_person = contact_person;
    }

    public String getContact() {
        return this.contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getJob_title() {
        return job_title;
    }

    public void setJob_title(String job_title) {
        this.job_title = job_title;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJobPortal() {
        return this.job_portal;
    }

    public void setJobPortal(String job_portal) {
        this.job_portal = job_portal;
    }

    public String getJob_description() {
        return job_description;
    }

    public void setJob_description(String job_description) {
        this.job_description = job_description;
    }

    public List<String> getBenefits() {
        return benefits;
    }

    public void setBenefits(List<String> benefits) {
        this.benefits = benefits;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getResponsibilities() {
        return responsibilities;
    }

    public void setResponsibilities(List<String> responsibilities) {
        this.responsibilities = responsibilities;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCompany_profile() {
        return company_profile;
    }

    public void setCompany_profile(String company_profile) {
        this.company_profile = company_profile;
    }
    public String toString(){
        return String.format(
            "Job ID: %d\n" +
            "Experience: %s\n" +
            "Qualification: %s\n" +
            "Salary Range: %s\n" +
            "Location: %s, %s\n" +
            "Coordinates: (%.4f, %.4f)\n" +
            "Work Type: %s\n" +
            "Company Size: %d\n" +
            "Job Posting Date: %s\n" +
            "Preference: %s\n" +
            "Contact Person: %s\n" +
            "Contact: %s\n" +
            "Job Title: %s\n" +
            "Role: %s\n" +
            "Job Portal: %s\n" +
            "Description: %s\n" +
            "Company: %s\n" +
            "Company Profile: %s\n" +
            "Benefits: %s\n" +
            "Skills: %s\n" +
            "Responsibilities: %s\n",
            job_id, experience, qualification, salary_range, location, country,
            latitude, longitude, work_type, company_size,
            job_posting_date != null ? job_posting_date.toString() : "N/A",
            preference, contact_person, contact, job_title, role, job_portal, job_description,
            company, company_profile,
            benefits != null ? String.join(", ", benefits) : "N/A",
            skills != null ? String.join(", ", skills) : "N/A",
            responsibilities != null ? String.join(", ", responsibilities) : "N/A"
        );
    }
    public void toBytes(DataOutputStream dos){
        try {
            dos.writeInt(getByteSize());
            //System.out.printf("ID: %d, Bytes: %d\n", this.job_id, getByteSize());
            dos.writeByte(1);
            dos.writeShort(this.job_id);
            dos.writeUTF(this.experience);
            dos.writeUTF(this.qualification);
            dos.writeUTF(this.salary_range);
            dos.writeUTF(this.location);
            dos.writeUTF(this.country);
            dos.writeFloat(this.latitude);
            dos.writeFloat(this.longitude);
            dos.writeUTF(this.work_type);
            dos.writeInt(this.company_size);
            dos.writeLong(this.job_posting_date.getEpochSecond());
            dos.writeByte(6);
            String preference = this.preference;
            while(preference.length() < 6) preference += " ";
            dos.write(preference.substring(0, 6).getBytes()); // STRING FIXA DE TAMANHO 6
            dos.writeUTF(this.contact_person);
            dos.writeUTF(this.contact);
            dos.writeUTF(this.job_title);
            dos.writeUTF(this.role);
            dos.writeUTF(this.job_portal);
            dos.writeUTF(this.job_description);
            writeListBinary(dos, this.benefits);
            writeListBinary(dos, this.skills);
            writeListBinary(dos, this.responsibilities);
            dos.writeUTF(this.company);
            dos.writeUTF(this.company_profile);
        } catch (Exception e) {
            System.err.println("Erro em Job.java, toBytes: ID = " + this.job_id + " Catch = " + e);
        }
    }
    public int getByteSize(){
        int size = 0;
        size += Short.BYTES; // job_id (short = 2 bytes)
        // System.out.printf("Job_ID Bytes: %d\n", Short.BYTES);
        size += Float.BYTES * 2; // latitude + longitude (float = 4 bytes cada)
        // System.out.printf("Latitude + Longitude Bytes: %d\n", Float.BYTES * 2);
        size += Integer.BYTES; // company_size (int = 4 bytes)
        // System.out.printf("Company_Size Bytes: %d\n", Integer.BYTES);
        size += Long.BYTES; // job_posting_date armazenado como timestamp (long = 8 bytes)
        // System.out.printf("Job_Posting_Date Bytes: %d\n", Long.BYTES);
        // Strings - considera o tamanho da string + os 2 bytes de tamanho
        size += getUtfSize(this.experience);
        // System.out.printf("Experience Bytes: %d\n", getUtfSize(experience));
        size += getUtfSize(this.qualification);
        // System.out.printf("Qualification Bytes: %d\n", getUtfSize(qualification));
        size += getUtfSize(this.salary_range);
        // System.out.printf("Salary_Range Bytes: %d\n", getUtfSize(salary_range));
        size += getUtfSize(this.location);
        // System.out.printf("Location Bytes: %d\n", getUtfSize(location));
        size += getUtfSize(this.country);
        // System.out.printf("Country Bytes: %d\n", getUtfSize(country));
        size += getUtfSize(this.work_type);
        // System.out.printf("Work_Type Bytes: %d\n", getUtfSize(work_type));
        size += getUtfSize(this.contact_person);
        // System.out.printf("Contact_Person Bytes: %d\n", getUtfSize(contact_person));
        size += getUtfSize(this.contact);
        // System.out.printf("Contact Bytes: %d\n", getUtfSize(contact));
        size += getUtfSize(this.job_title);
        // System.out.printf("Job_Title Bytes: %d\n", getUtfSize(job_title));
        size += getUtfSize(this.role);
        // System.out.printf("Role Bytes: %d\n", getUtfSize(role));
        size += getUtfSize(this.job_portal);
        // System.out.printf("Job_Portal Bytes: %d\n", getUtfSize(job_portal));
        size += getUtfSize(this.job_description);
        // System.out.printf("Job_Description Bytes: %d\n", getUtfSize(job_description));
        size += getUtfSize(this.company);
        // System.out.printf("Company Bytes: %d\n", getUtfSize(company));
        size += getUtfSize(this.company_profile);
        // System.out.printf("Company_Profile Bytes: %d\n", getUtfSize(company_profile));
        size += 8; // String fixa de tamanho 6 (preference) + 2 bytes 
        // System.out.printf("Preference Bytes: %d\n", 8);

        //Listas
        size += getListUtfSize(this.benefits);
        // System.out.printf("Benefits Bytes: %d\n", getListUtfSize(benefits));
        size += getListUtfSize(this.skills);
        // System.out.printf("Skills Bytes: %d\n", getListUtfSize(skills));
        size += getListUtfSize(this.responsibilities);
        // System.out.printf("Responsabilities Bytes: %d\n", getListUtfSize(responsibilities));
        // System.out.printf("Total: %d\n", size);
        return size;
    }
    private void writeListBinary(DataOutputStream dos, List<String> list) {
        if (list != null) {
            try {
                dos.writeInt(list.size());
                for (String item : list){
                    dos.writeUTF(item);
                }
            } catch (Exception e) {
                System.err.println("Erro em writeListBinary, Registro.java: " + e);
            }
        }
    }
    private int getListUtfSize(List<String> list) {
        int size = Integer.BYTES; // 4 bytes para o tamanho da lista
        for (String str : list) {
            size += getUtfSize(str); // Adiciona o tamanho de cada string na lista
        }
        return size;
    }
    private int getUtfSize(String str) {
        int utfLength = str.getBytes(StandardCharsets.UTF_8).length;
        return 2 + utfLength;
    }
}
