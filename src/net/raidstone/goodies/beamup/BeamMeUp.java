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
    private String permMessage = "";
    private String prefix = "&3&o";
    private String blocked = "The next platform is blocked by an object !";
    private String noplatform = "There is no platform to travel to in that direction !";
    private String travel = "Woosh !";
    private String permission = "beam.use";
    
    @Override
    public void onEnable()
    {
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        loadItem();
        
        permMessage = trans(permMessage);
        prefix = trans(prefix);
        blocked = trans(blocked);
        noplatform = trans(noplatform);
        travel = trans(travel);
    }
    
    @Override
    public void onDisable()
    {
    }
    
    /**
     * Small shortcut that allows to translate alternate color codes
     * @param m The message to translate
     * @return The translated message
     */
    private String trans(String m)
    {
        if(m!=null)
            m = ChatColor.translateAlternateColorCodes('&', m);
        return m;
    }
    
    /**
     * Reads the configuration to get the item
     */
    public void loadItem()
    {
        Material m = Material.getMaterial(this.getConfig().getString("item"));
        if(m!=null)
            item = m;
        String perm = this.getConfig().getString("messages.noperm");
        String pref = this.getConfig().getString("messages.prefix");
        String bloc = this.getConfig().getString("messages.blocked");
        String nopl = this.getConfig().getString("messages.noplat");
        String trav = this.getConfig().getString("messages.travel");
        String prm = this.getConfig().getString("permission");
        
        boolean save = false;
        if(prm!=null)
        {
            if(prm.equalsIgnoreCase("none")) permission=null;
            else permission = prm;
        } else save=true;
        
        if(perm!=null)
        {
            if(perm.equalsIgnoreCase("none")) permMessage=null;
            else permMessage=perm;
        } else save=true;
        
        if(pref!=null)
        {
            if(pref.equalsIgnoreCase("none")) prefix=null;
            else prefix=pref;
        } else save=true;
        
        if(bloc!=null)
        {
            if(bloc.equalsIgnoreCase("none")) blocked=null;
            else blocked=bloc;
        } else save=true;
        
        if(nopl!=null)
        {
            if(nopl.equalsIgnoreCase("none")) noplatform=null;
            else noplatform=nopl;
        } else save=true;
        
        if(trav!=null)
        {
            if(trav.equalsIgnoreCase("none")) travel=null;
            else travel=trav;
        } else save=true;
   
        if(save)
        {
            this.getConfig().options().copyDefaults(true);
            this.saveConfig();
        }
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
        Player p = event.getPlayer();
        Location l = p.getLocation();
        Action a = event.getAction();
    
        // Ignore the event if you don't have a watch in your main hand
        if(p.getInventory().getItemInMainHand().getType()!=item)
            return;
        
        // Check if the player is inside the beam.
        if(!isInsideBeam(l))
            return;
       
        // Ignore all actions but left/right click air
        if(a!=Action.LEFT_CLICK_AIR && a!=Action.RIGHT_CLICK_AIR)
            return;
        
    
        // No permission to use ? Lol, you're doomed.
        if(permission!=null && !event.getPlayer().hasPermission(permission))
        {
            if(permMessage!=null)
                event.getPlayer().sendMessage(permMessage);
            return;
        }
        
        // Check whether the player wants to go up or down
        boolean goUp = event.getAction()==Action.LEFT_CLICK_AIR;
        
        
        // Gets the next platform
        Location platform = findPlatform(l, goUp);
        if(platform==null)
        {
            if(noplatform!=null)
                p.sendMessage(prefix+noplatform);
            return;
        }
        
        // Checks if the next platform is safe or not
        if(!isSafe(platform))
        {
            if(blocked!=null)
                p.sendMessage(prefix+blocked);
            return;
        }
       
        // Teleport and play effects
        p.teleport(platform.add(0.5,1.2,0.5));
        if(travel!=null)
            p.sendMessage(prefix+travel);
        p.playSound(p.getLocation(),Sound.ENTITY_ENDERMEN_TELEPORT, 1, 1);
    }
    
    /**
     * Prevents a block from being broken while inside a beam
     * @param event BlockBreakEvent
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
        if(!event.getPlayer().hasPermission(permission))
            return;
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
        if(!event.getPlayer().hasPermission(permission))
            return;
        if(event.getPlayer().getInventory().getItemInMainHand().getType()!=item || !isInsideBeam(event.getPlayer().getLocation()))
            return;
        event.setCancelled(true);
    }
}
