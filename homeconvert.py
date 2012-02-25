import sqlite3, csv, sys, os, time

class NamedLocation(object):
    def __init__(self, name, owner, world, x, y, z, pitch, yaw):
        self.name = name
        self.owner = owner
        self.world = world
        self.x = x
        self.y = y
        self.z = z
        self.pitch = pitch
        self.yaw = yaw
        
def dump_commandbook_csv(warps, dest):
    with open(dest, 'a') as raw:
        locwriter = csv.writer(raw, quoting=csv.QUOTE_ALL)
        for warp in warps:
            locwriter.writerow([warp.name, warp.world, warp.owner, warp.x, warp.y, warp.z, warp.pitch, warp.yaw])
            
def importer_myhomes(args):
    if (len(args) < 1):
        raise Exception("No MyHomes path specified!")
    dbpath = os.path.abspath(os.path.expanduser(args[0]))
    conn = sqlite3.connect(dbpath)
    
    c = conn.cursor();
    c.execute("SELECT `name`, `world`, `x`, `y`,`z`,`pitch`,`yaw` FROM `homeTable`")
    warps = []
    for res in c:
        warps.append(NamedLocation(res[0], res[0], res[1], res[2], res[3], res[4], res[5], res[6]))
    c.close()
    return warps
    
importers = {
    "myhomes": importer_myhomes
}

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print "Not enough arguments. Usage: %s <converter> <destination file> [converter args]" % __file__
        exit(1)
    if not sys.argv[1] in importers:
        print "Unknown converter '%s' specified! Available converters: %s" % (sys.argv[1], importers.keys().__str__()[1:-1])
        exit(1)

    destpath = os.path.abspath(os.path.expanduser(sys.argv[2]))
    if not os.path.isdir(os.path.dirname(destpath)):
        os.makedirs(os.path.dirname(destpath))
        
    importer = importers[sys.argv[1]]
    warps = None

    try:
        warps = importer(sys.argv[3:])
    except Exception as e:
        print e
        exit(1)
  
    dump_commandbook_csv(warps, destpath)
    print "%d homes successfully converted!" % len(warps)