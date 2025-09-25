package com.bekvon.bukkit.residence.containers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.Zrips.CMILib.Logs.CMIDebug;

public class ResidencePlayerMaxValues {

    private static ConcurrentHashMap<UUID, ResidencePlayerMaxValues> data = new ConcurrentHashMap<UUID, ResidencePlayerMaxValues>();

    public static ResidencePlayerMaxValues get(UUID uuid) {
        return data.computeIfAbsent(uuid, k -> new ResidencePlayerMaxValues());
    }

    public static ResidencePlayerMaxValues getNullable(UUID uuid) {
        return data.get(uuid);
    }

    public static void updateUUID(UUID from, UUID to) {
        ResidencePlayerMaxValues value = data.remove(from);
        if (value != null)
            data.put(to, value);
    }

    private int maxRes = -1;
    private int maxRents = -1;
    private int maxSubzones = -1;
    private int maxSubzoneDepth = -1;
    private int maxX = -1;
    private int maxZ = -1;

    public int getMaxRes() {
        return maxRes;
    }

    public void setMaxRes(int maxRes) {
        this.maxRes = maxRes;
    }

    public int getMaxRents() {
        return maxRents;
    }

    public void setMaxRents(int maxRents) {
        this.maxRents = maxRents;
    }

    public int getMaxSubzones() {
        return maxSubzones;
    }

    public void setMaxSubzones(int maxSubzones) {
        this.maxSubzones = maxSubzones;
    }

    public int getMaxSubzoneDepth() {
        return maxSubzoneDepth;
    }

    public void setMaxSubzoneDepth(int maxSubzoneDepth) {
        this.maxSubzoneDepth = maxSubzoneDepth;
    }

    public int getMaxX() {
        return maxX;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
    }

    public Map<String, Object> serialize() {

        Map<String, Object> max = new HashMap<String, Object>();

        Map<String, Object> map = new HashMap<String, Object>();

        if (maxRes != -1)
            map.put("Res", maxRes);

        if (maxRents != -1)
            map.put("Rents", maxRents);

        if (maxSubzones != -1)
            map.put("Subzones", maxSubzones);

        if (maxSubzoneDepth != -1)
            map.put("SubzoneDepth", maxSubzoneDepth);

        if (maxX != -1)
            map.put("X", maxX);

        if (maxZ != -1)
            map.put("Z", maxZ);

        max.put("Max", map);

        return max;
    }

    public static ResidencePlayerMaxValues deserialize(UUID uuid, Map<String, Object> max) {

        if (max == null)
            return null;

        if (!max.containsKey("Max"))
            return null;

        Map<String, Object> map = (Map<String, Object>) max.get("Max");

        if (map == null)
            return null;

        int maxRes = -1;
        int maxRents = -1;
        int maxSubzones = -1;
        int maxSubzoneDepth = -1;
        int maxX = -1;
        int maxZ = -1;

        if (map.containsKey("Res"))
            maxRes = (Integer) map.get("Res");

        if (map.containsKey("Rents"))
            maxRents = (Integer) map.get("Rents");

        if (map.containsKey("Subzones"))
            maxSubzones = (Integer) map.get("Subzones");

        if (map.containsKey("SubzoneDepth"))
            maxSubzoneDepth = (Integer) map.get("SubzoneDepth");

        if (map.containsKey("X"))
            maxX = (Integer) map.get("X");

        if (map.containsKey("Z"))
            maxZ = (Integer) map.get("Z");

        if (maxRes != -1 || maxRents != -1 || maxSubzones != -1 || maxSubzoneDepth != -1 || maxX != -1 || maxZ != -1) {
            ResidencePlayerMaxValues values = get(uuid);
            values.setMaxRes(maxRes);
            values.setMaxRents(maxRents);
            values.setMaxSubzones(maxSubzones);
            values.setMaxSubzoneDepth(maxSubzoneDepth);
            values.setMaxX(maxX);
            values.setMaxZ(maxZ);
            return values;
        }

        return null;
    }

}
