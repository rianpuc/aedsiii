package com.aedsiii.puc.model;
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
        this.job_id = 0;
        this.latitude = this.longitude = 0;
        this.experience = this.qualification = this.salary_range = this.location = this.country = this.work_type = this.contact = this.job_portal =
        this.preference = this.contact_person = this.job_title = this.role = this.job_description = this.company = this.company_profile = null;
        this.skills = this.responsibilities = null;
        this.job_posting_date = null;
        this.benefits = null;
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
    public void mostrar(){
        System.out.printf(
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
}
