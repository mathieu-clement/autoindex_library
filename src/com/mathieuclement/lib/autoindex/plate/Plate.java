package com.mathieuclement.lib.autoindex.plate;

import com.mathieuclement.lib.autoindex.canton.Canton;

public class Plate {
    private int number;
    private Canton canton;
    private PlateType type;

    public Plate() {
    }

    public Plate(int number, PlateType type, Canton canton) {
        this.number = number;
        this.type = type;
        this.canton = canton;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Canton getCanton() {
        return canton;
    }

    public void setCanton(Canton canton) {
        this.canton = canton;
    }

    public PlateType getType() {
        return type;
    }

    public void setType(PlateType type) {
        this.type = type;
    }

    public String toString() {
        return this.canton + " " + this.number + " (" + this.type + ")";
    }

    // TODO Implement hashCode() and equals()
}
