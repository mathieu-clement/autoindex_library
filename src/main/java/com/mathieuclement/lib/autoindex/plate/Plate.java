package com.mathieuclement.lib.autoindex.plate;

import com.mathieuclement.lib.autoindex.canton.Canton;

/**
 * A plate. (car, motorcycle, etc.)
 */
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

    /**
     * Format plate number by adding a space if appropriate. Such as:
     * 123      => "123"
     * 1234     => "1234"
     * 12345    => "12 345"
     * 123456   => "123 456"
     *
     * @param number plate number
     * @return number formatted as string with a space in the middle if needed
     */
    public static String formatNumber(int number) {

        if (number < 1 || number > 999999) {
            throw new IllegalArgumentException(number + " is not a valid plate number!");
        }

        String numberStr = String.valueOf(number);

        // Don't do anything is number has less than 5 digits
        if (number < 10000) {
            return numberStr;
        }

        // If number has 5 digits, take first two digits, otherwise take first three digits
        String partBeforeSpace, partAfterSpace;
        if (number < 100000) {
            partBeforeSpace = numberStr.substring(0, 2);
            partAfterSpace = numberStr.substring(2, 5);
        } else {
            partBeforeSpace = numberStr.substring(0, 3);
            partAfterSpace = numberStr.substring(3, 6);
        }

        return partBeforeSpace + " " + partAfterSpace;
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
