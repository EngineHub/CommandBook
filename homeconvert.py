#!/usr/bin/env python
import sqlite3
import csv
import sys
import os
import logging


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


class CommandBookLocationsDatabase(object):
    def read(self, src):
        with open(src, 'r') as raw:
            locreader = csv.reader(raw)
            rownum = 0
            warps = []
            for row in locreader:
                rownum += 1
                if len(row) < 8:
                    logging.warning("Line %d has < 8 rows: %s" % (rownum, row))
                    continue
                warps.append(NamedLocation(row[0], row[2], row[1], row[3],
                                            row[4], row[5], row[6], row[7]))
            return warps

    def write(self, warps, dest):
            with open(dest, 'a') as raw:
                locwriter = csv.writer(raw, quoting=csv.QUOTE_ALL)
                for warp in warps:
                    locwriter.writerow([warp.name, warp.world, warp.owner,
                                        warp.x, warp.y, warp.z, warp.pitch,
                                        warp.yaw])


class MyHomesLocationsDatabase(object):
    def read(self, src):
        conn = sqlite3.connect(src)
        c = conn.cursor()
        c.execute("SELECT `name`, `world`, `x`, `y`,`z`,`pitch`,`yaw` FROM `homeTable`")

        warps = []
        for res in c:
            warps.append(NamedLocation(res[0], res[0], res[1], res[2], res[3], res[4], res[5], res[6]))
        c.close()
        return warps

    def write(self, warps, dest):
        raise Exception("MyHomes does not support exporting!")


class Warpz0rLocationsDatabase(object):
    def read(self, src):
        with open(src) as db:
            warps = []
            linenum = 0
            for line in db:
                linenum += 1
                split = line.split(":")
                if len(split) < 6:
                    logging.warning("Less than required 6 entries on line %d: %s" % (linenum, line))
                    continue
                warps.append(NamedLocation(split[0], split[0], split[5], split[1], split[2], split[3], 0, split[4]))
            return warps

    def write(self, warps, dest):
        with open(dest, 'a') as db:
            for warp in warps:
                db.write(":".join([warp.name, warp.x, warp.y, warp.z, warp.yaw, warp.world, '-1']) + '\n')

importers = {
    "myhomes": MyHomesLocationsDatabase,
    "warpz0r": Warpz0rLocationsDatabase,
    "cmdbook": CommandBookLocationsDatabase
}

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print "Not enough arguments. Usage: %s <source format> <source file> <destination format> <destination file>" % __file__
        exit(1)
    if not sys.argv[1] in importers:
        print "Unknown converter '%s' specified! Available converters: %s" % (sys.argv[1], importers.keys().__str__()[1:-1])
        exit(1)
    if not sys.argv[3] in importers:
        print "Unknown converter '%s' specified! Available converters: %s" % (sys.argv[3], importers.keys().__str__()[1:-1])
        exit(1)

    logging.basicConfig(format='[%(asctime)s] [HomeConverter] %(message)s', datefmt='%H:%M:%S')

    destpath = os.path.abspath(os.path.expanduser(sys.argv[4]))
    srcpath = os.path.abspath(os.path.expanduser(sys.argv[2]))
    if not os.path.isdir(os.path.dirname(srcpath)):
        logging.error("Source file (%s) does not exist!")
    if not os.path.isdir(os.path.dirname(destpath)):
        os.makedirs(os.path.dirname(destpath))
    importer = importers[sys.argv[1]]()
    output = importers[sys.argv[3]]()
    warps = None

    try:
        warps = importer.read(srcpath)
        output.write(warps, destpath)
        print "%d homes successfully converted!" % len(warps)
    except Exception, e:
        logging.error(e)
        exit(1)
