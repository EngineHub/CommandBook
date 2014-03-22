package com.sk89q.commandbook.profiles.binary;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.profiles.ProfileManager;
import com.sk89q.commandbook.profiles.ProfileSettings;
import com.sk89q.commandbook.profiles.UnversionedProfile;
import com.sk89q.commandbook.profiles.editions.Profile_E1;
import org.bukkit.entity.Player;

import java.io.*;

public class BinaryProfileManager extends ProfileManager {

    private final String workingDir = CommandBook.inst().getDataFolder().getPath() + "/profiles/";

    @Override
    public Profile_E1 createProfile(Player player, ProfileSettings settings) {
        return BinaryProfileFactory.createProfile(player, settings);
    }

    @Override
    public Profile_E1 getProfile(String domain, String profileName) {

        // Handle the actual file information
        File k = getFile(domain, profileName);
        if (!k.exists()) return null;

        // Begin reading in the file as an object
        Object o = null;

        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = new FileInputStream(k);
            ois = new ObjectInputStream(fis);
            o = ois.readObject();
        } catch (FileNotFoundException e) {
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) { }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException ignored) { }
            }
        }

        // Check that the object is what we actually want
        if (!(o instanceof UnversionedProfile)) {
            // TODO throw new something
            return null;
        }

        // Latest edition
        if (o instanceof Profile_E1) {
            return (Profile_E1) o;
        }
        return null;
    }

    @Override
    public boolean saveProfile(String domain, String profileName, Profile_E1 profile) {

        File file = getFile(domain, profileName);

        // Delete any old files
        if (file.exists() && !file.delete()) {
            return false;
        }

        // Ensure the directory is accessible and the file exist
        try {
            if (!file.mkdirs() || !file.createNewFile()) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        FileOutputStream fos = null;
        ObjectOutputStream oss = null;

        try {
            fos = new FileOutputStream(file);
            oss = new ObjectOutputStream(fos);
            oss.writeObject(profile);
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) { }
            }
            if (oss != null) {
                try {
                    oss.close();
                } catch (IOException ignored) { }
            }
        }
        return true;
    }

    @Override
    public boolean deleteProfile(String domain, String profileName) {

        File k = getFile(domain, profileName);
        if (k.exists()) {
            k.delete();
        }
        return k.exists();
    }

    private File getFile(String domain, String profileName) {
        return new File(workingDir + domain + "/" + profileName + ".dat");
    }
}
