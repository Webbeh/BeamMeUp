package net.raidstone.goodies.beamup;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Weby@we-bb.com <Nicolas Glassey>
 * @version 1.0.0
 * @since 13/11/16
 */
public class BeamMeUp extends JavaPlugin implements Listener
{
    
    private Material item = Material.WATCH;
    
    @Override
    public void onEnable()
    {
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        loadItem();
    }
    
    @Override
    public void onDisable()
    {
    }
    
    /**
     * Reads the configuration to get the item
     */
    public void loadItem()
    {
        Bukkit.getLogger().info("Getting material");
        Material m = Material.getMaterial(this.getConfig().getString("item"));
        Bukkit.getLogger().info("Determined to be : "+m);
        if(m!=null)
            item = m;
    }
    
    /**
     * Finds out whether the player is inside a beacon's beam or not
     * @param l The player's location
     * @return True if the player's location is inside a beam
     */
    private boolean isInsideBeam(Location l)
    {
        for(int y = l.getBlockY(); y>=0; y--)
        {
            Block b = l.getWorld().getBlockAt(l.getBlockX(),y,l.getBlockZ());
            if(b.getType()== Material.BEACON)
                return true;
        }
        return false;
    }
    
    /**
     * Finds the next platform to teleport to
     * @param l The player's location
     * @param goUp True if the player wants to go up
     * @return The next platform's location, or null.
     */
    private Location findPlatform(Location l, boolean goUp)
    {
    
        //Prevent any movement up/down if you're next to the borders
        if((l.getBlockY()>=254 && goUp) || (l.getBlockY()<=2 && !goUp)) return null;
    
        for(int y = goUp?l.getBlockY():l.getBlockY()-2; y>=0 && y<=255; y=goUp?y+1:y-1)
        {
            //Look for stained glass or beacon block to teleport to
            if(l.getWorld().getBlockAt(l.getBlockX(),y,l.getBlockZ()).getType()==Material.STAINED_GLASS || l.getWorld().getBlockAt(l.getBlockX(), y, l.getBlockZ()).getType()==Material.BEACON)
                return new Location(l.getWorld(), l.getBlockX(), y, l.getBlockZ());
        }
        return null;
    }
    
    /**
     * Checks if the next platform is safe to land on or not. It's considered safe if there is only air in the two blocks above it
     * @param l The next platform's location
     * @return True if the platform is safe.
     */
    private boolean isSafe(Location l)
    {
        
        for(int y = l.getBlockY()+1; y<=l.getBlockY()+2;y++)
        {
            if(l.getWorld().getBlockAt(l.getBlockX(),y,l.getBlockZ()).getType()!=Material.AIR)
                return false;
        }
        return true;
    }
   
    /**
     * The main event
     * @param event PlayerInteractEvent
     */
    @EventHandler
    public void onClockClick(PlayerInteractEvent event)
    {
        // Ignore all actions but left/right click air
        if(event.getAction()!=Action.LEFT_CLICK_AIR && event.getAction()!=Action.RIGHT_CLICK_AIR)
            return;
        
        // Ignore the event if you don't have a watch in your main hand
        if(event.getPlayer().getInventory().getItemInMainHand().getType()!=item)
            return;
    
        Player p = event.getPlayer();
        Location l = p.getLocation();
    
        // Check if the player is inside the beam.
        if(!isInsideBeam(l))
            return;
       
        // Check whether the player wants to go up or down
        boolean goUp = event.getAction()==Action.LEFT_CLICK_AIR;
        
        
        // Gets the next platform
        Location platform = findPlatform(l, goUp);
        if(platform==null)
        {
            p.sendMessage("There is no platform to travel to !");
            return;
        }
        
        // Checks if the next platform is safe or not
        if(!isSafe(platform))
        {
            p.sendMessage("The next platform is blocked !");
            return;
        }
       
        // Teleport and play effects
        p.teleport(platform.add(0.5,1.2,0.5));
        p.sendMessage(ChatColor.BLUE+""+ChatColor.ITALIC+"Woosh !");
        p.playSound(p.getLocation(),Sound.ENTITY_ENDERMEN_TELEPORT, 1, 1);
    }
    
    /**
     * Prevents a block from being broken while inside a beam
     * @param event BlockBreakEvent
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
        if(event.getPlayer().getInventory().getItemInMainHand().getType()!=item || !isInsideBeam(event.getPlayer().getLocation()))
            return;
        event.setCancelled(true);
    }
    
    /**
     * Prevents block placement while inside the beam holding the item
     * @param event BlockPlaceEvent
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if(event.getPlayer().getInventory().getItemInMainHand().getType()!=item || !isInsideBeam(event.getPlayer().getLocation()))
            return;
        event.setCancelled(true);
    }
}
