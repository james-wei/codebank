from sim.api import *
from sim.basics import *

class RIPRouter (Entity):

    # Maximum path length (count-to-infinity):
    POSITIVE_INFINITY = 100

    def __init__(self):
        """Initialize empty routing/forwarding tables. Initialize empty 
        neighbors dictionary."""
        self.routing_table = {}         # Neighbor => Destination => Distance
        self.neighbors = {}             # Neighbor => Port number
        self.forwarding_table = {}      # Destination => {Distance, Port}

    def handle_rx (self, packet, port):
        """Packet handling at this RIPRouter. DiscoveryPacket and RoutingUpdate
        packets may modify routing, forwarding, and neighbors tables. Regular
        packets are forwarded towards their respective destinations."""
        if type(packet) is DiscoveryPacket:
            self.handle_discovery_packet(packet, port)
        elif type(packet) is RoutingUpdate and packet.src in self.routing_table:
            self.handle_routing_update(packet, port)
        elif packet.ttl >= 0 and packet.dst is not self:
            self.send(packet, self.get_port(packet.dst), False)

    def get_port(self, destination):
        """Given a destination of type Entity, return the port number to which
            a packet should be sent through in order to reach the destination."""
        if destination in self.forwarding_table and \
           self.forwarding_table[destination]["distance"] < RIPRouter.POSITIVE_INFINITY:
            return self.forwarding_table[destination]["port"]
        else:
            # No known way of reaching the destination from this RIPRouter:
            return None

    def handle_discovery_packet(self, packet, port):
        """Handling for DiscoveryPacket packets."""
        if packet.is_link_up:
            self.add_neighbor(packet, port)
        else:
            self.remove_neighbor(packet, port)

    def add_neighbor(self, pkt, port):
        """Handle the presence of a new neighboring node."""
        # Add the new neighbor to the destinations list of each existing 
        # neighbor:
        for nbr in self.routing_table:
            if pkt.src not in self.routing_table[nbr]:
                self.routing_table[nbr][pkt.src] = RIPRouter.POSITIVE_INFINITY
        # Create a column for the new neighbor in the routing table:
        new_rtgTable_entry = {}
        new_rtgTable_entry[pkt.src] = 1
        for neighbor in self.neighbors:
            new_rtgTable_entry[neighbor] = RIPRouter.POSITIVE_INFINITY
        self.routing_table[pkt.src] = new_rtgTable_entry
        # Create an entry in the forwarding table for the new neighbor:
        new_fwdingTable_entry = {"port": port, "distance": 1}
        self.forwarding_table[pkt.src] = new_fwdingTable_entry
        # Add new neighbor to neighbors list:
        self.neighbors[pkt.src] = port
        # Send a routing update to all neighbors to notify them of changes:
        self.send_routing_update()

    def remove_neighbor(self, pkt, port):
        """Handle the removal of a neighboring node."""
        dv_has_changed = False
        # Remove the neighbor from routing table & neighbors table:
        self.routing_table.pop(pkt.src, None)
        self.neighbors.pop(pkt.src, None)
        # Update forwarding table:
        
        nodes_to_delete = []

        for dest in self.forwarding_table:
            # If currently routing through deleted neighbor, find a replacement
            if self.forwarding_table[dest]["port"] is port:
                new_port, new_dist = self.find_shortest_path(dest)
                if new_dist != self.forwarding_table[dest]["distance"]:
                    dv_has_changed = True
                if new_port is not None:
                    self.forwarding_table[dest]["port"] = new_port
                    self.forwarding_table[dest]["distance"] = new_dist
                else:
                    nodes_to_delete.append(dest)

        for node in nodes_to_delete:
            self.forwarding_table.pop(node, None)

        if dv_has_changed:
            self.send_routing_update()

    def find_shortest_path(self, dest):
        """Find shortest path from this RIPRouter to the destination. Return a
        tuple containing a port number and the shortest distance."""
        min_dist = RIPRouter.POSITIVE_INFINITY
        min_port = None
        for nbr in self.routing_table:
            curr_dist = self.routing_table[nbr][dest]
            if curr_dist != RIPRouter.POSITIVE_INFINITY:
                if curr_dist < min_dist:
                    min_dist = curr_dist
                    min_port = self.neighbors[nbr]
                elif curr_dist == min_dist and self.neighbors[nbr] < min_port:
                    min_port = self.neighbors[nbr]        
        return (min_port, min_dist)

    def send_routing_update(self):
        """Send a RoutingUpdate packet to each neighbor."""
        for nbr in self.neighbors:
            if issubclass(type(nbr), HostEntity):
                continue
            # Create/configure a new RoutingUpdate packet for each neighbor:
            rtg_updt = RoutingUpdate()
            rtg_updt.src = self
            rtg_updt.dst = nbr
            # Construct unique DV to be sent to each neighbor:
            for dst in self.forwarding_table:
                if dst is nbr:
                    # Do nothing -- don't add neighbor to DV in update packet:
                    continue
                # The case where I am currently routing through my neighbor to go to another node
                elif self.forwarding_table[dst]["port"] == self.neighbors[nbr]:
                    # Poison Reverse:
                    rtg_updt.add_destination(dst, RIPRouter.POSITIVE_INFINITY)
                else:
                    rtg_updt.add_destination(dst, self.forwarding_table[dst]["distance"])
            self.send(rtg_updt, self.get_port(nbr))

    def handle_routing_update(self, packet, port):
        """Handling for RoutingUpdate packets."""
        nbr_entry = self.routing_table[packet.src]  # Destination => Distance
        
        # Find new nodes by taking the difference of two sets:
        new_nodes = set(packet.paths) - set(nbr_entry.keys())

        # Update tables, add new nodes. Determine if DV has changed:
        
        # Update old nodes:
        a = self.update_tables(nbr_entry, packet, port)
        
        # Update new nodes:
        b = self.add_nodes(new_nodes, nbr_entry, packet, port)

        dv_has_changed = a or b

        if dv_has_changed:
            self.send_routing_update()

    def update_tables(self, nbr_entry, pkt, prt):
        """Given a RouterUpdate packet, modify the routing and forwarding
        tables accordingly."""
        dv_has_changed = False
        for dest in nbr_entry:
            # Update routing table:
            if dest is not pkt.src and dest is not self:
                if dest not in pkt.paths: # Implicit withdrawal
                    nbr_entry[dest] = RIPRouter.POSITIVE_INFINITY
                else:
                    if pkt.paths[dest] >= RIPRouter.POSITIVE_INFINITY:
                        nbr_entry[dest] = RIPRouter.POSITIVE_INFINITY
                    else:    
                        nbr_entry[dest] = pkt.paths[dest] + 1

            # Update forwarding table:
            if (self.update_forwarding_table(nbr_entry, dest, prt)):
                dv_has_changed = True

        return dv_has_changed

    def update_forwarding_table(self, nbr_entry, dest, port):
        """Given a RouterUpdate packet or the presence of a new node, modify
        the forwarding table accordingly."""
        dv_has_changed = False

        if dest not in self.forwarding_table:
            return dv_has_changed
        elif nbr_entry[dest] < self.forwarding_table[dest]["distance"]:
            self.forwarding_table[dest]["port"] = port
            self.forwarding_table[dest]["distance"] = nbr_entry[dest]
            dv_has_changed = True
        elif nbr_entry[dest] == self.forwarding_table[dest]["distance"] and \
             port < self.forwarding_table[dest]["port"]:
            self.forwarding_table[dest]["port"] = port
            dv_has_changed = False
        elif self.forwarding_table[dest]["port"] == port and \
             nbr_entry[dest] > self.forwarding_table[dest]["distance"]:
            # Fastest path was previously through this neighbor -- recalculate
            # shortest path
            min_port, min_dist = self.find_shortest_path(dest)
            dv_has_changed = True
            if min_port is not None:
                self.forwarding_table[dest]["port"] = min_port
                self.forwarding_table[dest]["distance"] = min_dist
            else:
                self.forwarding_table.pop(dest, None)
        return dv_has_changed

    def add_nodes(self, new_nodes, nbr_entry, packet, port):
        """Add new nodes that appear in a RouterUpdate packet that aren't 
        already in the routing/forwarding tables of this RIPRouter."""
        dv_has_changed = False
        for node in new_nodes:
            if packet.paths[node] >= RIPRouter.POSITIVE_INFINITY:
                nbr_entry[node] = RIPRouter.POSITIVE_INFINITY
            else:
                nbr_entry[node] = packet.paths[node] + 1
            for nbr in self.routing_table:
                if node not in self.routing_table[nbr]:
                    self.routing_table[nbr][node] = RIPRouter.POSITIVE_INFINITY
            if node not in self.forwarding_table:
                new_fTable_entry = {"port": port, "distance": nbr_entry[node]}
                self.forwarding_table[node] = new_fTable_entry
                dv_has_changed = True
            else:
                if (self.update_forwarding_table(nbr_entry, node, port)):
                    dv_has_changed = True
        return dv_has_changed
