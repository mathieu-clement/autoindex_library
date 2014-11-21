package com.mathieuclement.lib.autoindex.plate;

import java.util.regex.Pattern;

public class PlateOwner {
    protected String name = "";
    protected String address = "";
    protected int zip = -1;
    protected String town = "";
    private String addressComplement = "";

    // TODO Support éàëäë, etc.
    private Pattern wordsPattern = Pattern.compile("\\w+(-\\w+)*(\\s\\w+(-\\w+)*)*");

    public PlateOwner() {
    }

    public PlateOwner(String name, String address, String addressComplement, int zip, String town) {
        this.name = name;
        this.address = address;
        this.addressComplement = addressComplement;
        this.zip = zip;
        this.town = town;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void checkName() throws PlateOwnerDataException {
        if (!wordsPattern.matcher(name).matches())
            throw new PlateOwnerDataException("Name '" + name + "' is not valid.", this);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void checkAddress() throws PlateOwnerDataException {
        if (!("".equals(address) || wordsPattern.matcher(address).matches())) {
            throw new PlateOwnerDataException("Address '" + address + "' is not valid.", this);
        }
    }

    public int getZip() {
        return zip;
    }

    public void setZip(int zip) {
        this.zip = zip;
    }

    public void checkZip() throws PlateOwnerDataException {
        /*
        if (zip == -1 || zip > 9999) {
            throw new PlateOwnerDataException("ZIP '" + zip + "' is not a valid value or is not set.", this);
        }
        */
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public void checkTown() throws PlateOwnerDataException {
        if (!wordsPattern.matcher(town).matches()) {
            throw new PlateOwnerDataException("Town '" + town + "' is not valid.", this);
        }
    }

    public void check() throws PlateOwnerDataException {
        // TODO Enable when ready
//        checkName();
//        checkAddress();
//        checkZip();
//        checkTown();
    }

    @Override
    public String toString() {
        return "PlateOwner{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", zip=" + zip +
                ", town='" + town + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return 2 * name.hashCode() + 3 * address.hashCode() + 4 * zip + 5 * town.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlateOwner) {
            PlateOwner otherPlateOwner = (PlateOwner) obj;
            return this.name.equals(otherPlateOwner.name) &&
                    this.address.equals(otherPlateOwner.address) &&
                    this.zip == otherPlateOwner.zip &&
                    this.town.equals(otherPlateOwner.town);
        }
        return false;
    }

    public void setAddressComplement(String addressComplement) {
        this.addressComplement = addressComplement;
    }

    public String getAddressComplement() {
        return addressComplement;
    }
}
