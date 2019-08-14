package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

public class Data implements Serializable {

    String msg;
    String type;

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSender_port(int sender_port) {
        this.sender_port = sender_port;
    }

    public void setSuggested_seq(double suggested_seq) {
        this.suggested_seq = suggested_seq;
    }

    public void setSuggester_port(int suggester_port) {
        this.suggester_port = suggester_port;
    }

    public void setDeliverable(boolean deliverable) {
        this.deliverable = deliverable;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setMax_agreed(double max_agreed) {
        this.max_agreed = max_agreed;
    }

    public void setFailed_port(int failed_port) {
        this.failed_port = failed_port;
    }

    int sender_port;
    double suggested_seq;
    int suggester_port;
    boolean deliverable;
    int count;
    double max_agreed;
    int failed_port;

    public String getMsg() {
        return msg;
    }

    public String getType() {
        return type;
    }

    public int getSender_port() {
        return sender_port;
    }

    public double getSuggested_seq() {
        return suggested_seq;
    }

    public int getSuggester_port() {
        return suggester_port;
    }

    public boolean isDeliverable() {
        return deliverable;
    }

    public int getCount() {
        return count;
    }

    public double getMax_agreed() {
        return max_agreed;
    }

    public int getFailed_port() {
        return failed_port;
    }



}
