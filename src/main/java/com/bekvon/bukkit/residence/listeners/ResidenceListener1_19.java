    private void breakActor(InventoryHolder actor) {

        if (actor == null) {
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (actor instanceof Hopper) {
                Hopper hopper = (Hopper) actor;
                hopper.getBlock().breakNaturally();
            }
        }, 1);
    }

    private void breakSource(Inventory hopperInventory) {

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            Location hopperLoc = hopperInventory.getLocation();
            if (hopperLoc == null)
                return;
            Block block = hopperLoc.getBlock();
            // Only hopper
            if (block.getType() != Material.HOPPER)
                return;
            block.breakNaturally();
        }, 1);
    }

    private void breakDest(Inventory hopperInventory) {

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            Location hopperLoc = hopperInventory.getLocation();
            if (hopperLoc == null)
                return;
            Block block = hopperLoc.getBlock();
            // Only hopper
            if (block.getType() != Material.HOPPER)
                return;
            block.breakNaturally();
        }, 1);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHopperCrossRes(InventoryMoveItemEvent event) {

        Inventory source = event.getSource();
        Inventory dest = event.getDestination();
        if (source == null || dest == null)
            return;

        ClaimedResidence sourceRes = ClaimedResidence.getByLoc(source.getLocation());
        ClaimedResidence destRes = ClaimedResidence.getByLoc(dest.getLocation());
        InventoryHolder actor = event.getInitiator().getHolder();

        // source & dest not in Same Residence
        if (sourceRes != null && destRes != null && !sourceRes.equals(destRes)) {
            event.setCancelled(true);
            breakActor(actor);
            return;
        }
        // source in Res, dest not in Res
        if (sourceRes != null && destRes == null) {
            event.setCancelled(true);
            breakDest(dest);
            return;
        }
        // dest in Res, source not in Res
        if (sourceRes == null && destRes != null) {
            event.setCancelled(true);
            breakSource(source);
        }
        // ignore source & dest not in Res
    }
