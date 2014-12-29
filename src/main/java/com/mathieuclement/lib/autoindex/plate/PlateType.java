package com.mathieuclement.lib.autoindex.plate;

/**
 * Number plate type.<br/>
 * Can be used as an {@link Enum}.
 */
public final class PlateType {
    private String name;

    public PlateType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static PlateType[] values() {
        return new PlateType[]{
                AUTOMOBILE, AUTOMOBILE_TEMPORARY, AUTOMOBILE_BROWN, AUTOMOBILE_REPAIR_SHOP,
                MOTORCYCLE, MOTORCYCLE_TEMPORARY, MOTORCYCLE_YELLOW, MOTORCYCLE_BROWN, MOTORCYCLE_REPAIR_SHOP,
                MOPED,
                AGRICULTURAL,
                INDUSTRIAL
        };
    }

    public static final PlateType AUTOMOBILE = new PlateType("automobile");
    public static final PlateType AUTOMOBILE_BROWN = new PlateType("automobile_brown");
    public static final PlateType AUTOMOBILE_TEMPORARY = new PlateType("automobile_temporary");
    // Garage (plate number ends with "U")
    public static final PlateType AUTOMOBILE_REPAIR_SHOP = new PlateType("automobile_repair_shop");

    public static final PlateType MOTORCYCLE = new PlateType("motorcycle");
    public static final PlateType MOTORCYCLE_YELLOW = new PlateType("motorcycle_yellow");
    public static final PlateType MOTORCYCLE_BROWN = new PlateType("motorcycle_brown");
    public static final PlateType MOTORCYCLE_TEMPORARY = new PlateType("motorcycle_temporary");
    public static final PlateType MOTORCYCLE_REPAIR_SHOP = new PlateType("motorcycle_repair_shop");

    public static final PlateType MOPED = new PlateType("moped");
    public static final PlateType AGRICULTURAL = new PlateType("agricultural");
    public static final PlateType INDUSTRIAL = new PlateType("industrial");

    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PlateType plateType = (PlateType) o;

        return name.equals(plateType.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
