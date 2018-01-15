import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Hashtable;

/// User interface for application
public class MusicalWallpaperUI extends JFrame {

    private JPanel[] panels = new JPanel[] {
            new SelectPlaylistPanel(),
            new ArtworkSizePanel(),
            new LoadingScreenPanel(),
            new WallpaperSetPanel()
    };
    private int panelPosition = 0;

    public void setup() {
        setSize(400, 300);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Musical Wallpaper");

        setVisible(true);
        setResizable(false);
        setLocationRelativeTo(null);

        setupPanels(); // add empty borders to all panels

        // display the first panel
        add(panels[panelPosition]);
        pack();
    }

    private class SelectPlaylistPanel extends JPanel {
        SelectPlaylistPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(new JLabel("<html>Welcome to Musical Wallpaper" +
                    "<br>Make your favourite album covers into your new favourite wallpaper!" +
                    "<br>Create or find a playlist with your favourite albums then paste the link below with Ctrl-V"));
            add(new JLabel("URL of playlist on spotify.com:"));

            String initialURL = "";
            try {
                initialURL = PropertiesManager.getProperty("playlistURL");
                // use a playlist from spotify if this is the user's first time
                if (initialURL == null || initialURL.equals("")) {
					initialURL = "https://open.spotify.com/user/playlistmeukfeatured/playlist/0F2RaOrNectaIorC71tBQJ";
                }
            } catch(IOException e) {
                showErrorMessage(e.getMessage());
            } 
            final JTextField textField = new JTextField(initialURL);
            add(textField);

            JButton button = new JButton("Go");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        // try and get the playlistID and userID - if this works then continue to next panel
                        PlaylistIDManager.getPlaylistIDAndUserIDFromURL(textField.getText());
                        try {
                            PropertiesManager.setProperty("playlistURL", textField.getText());
                        } catch(IOException exception) {
                            showErrorMessage(exception.getMessage());
                        }
                        nextPanel();

                    } catch(InvalidPlaylistURLException exception) {
                        // an invalid URL was supplied, don't continue
                        showErrorMessage("That's not a valid spotify.com playlist URL");
                    }
                }
            });
            add(button);
        }
    }

    private class ArtworkSizePanel extends JPanel {
        ArtworkSizePanel() {
			//GUI
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(new JLabel("How many album covers per wallpaper?"));

            Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
            labelTable.put(new Integer(0), new JLabel("Few"));
            labelTable.put(new Integer(1), new JLabel("Some"));
            labelTable.put(new Integer(2), new JLabel("Many"));
            labelTable.put(new Integer(3), new JLabel("Rank Mode"));
            labelTable.put(new Integer(4), new JLabel("Zune Mode"));
            //TODO zune mode change picture size

            int initialValue = 0; // default in case of error
            try {
                initialValue = Integer.parseInt(PropertiesManager.getProperty("imageSizeCode", "0"));
            } catch(IOException e) {
                showErrorMessage("Could not load user preferences. " + e.getMessage());
            }

            final JSlider slider = new JSlider(0, 4, initialValue);
            slider.setLabelTable(labelTable);
            slider.setPaintLabels(true);
            slider.setPaintTicks(true);
            add(slider);
            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (!slider.getValueIsAdjusting()) {
                        try {
                            PropertiesManager.setProperty("imageSizeCode", String.valueOf(slider.getValue()));
                        } catch (IOException exception) {
                            showErrorMessage(exception.getMessage());
                        }
                    }
                }
            });

            JButton button = new JButton("Generate wallpapers");
            add(button);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    nextPanel();
                }
            });
        }
    }

    private class LoadingScreenPanel extends JPanel {

        private final JProgressBar progressBar;

        LoadingScreenPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            progressBar = new JProgressBar(0, 100);
            progressBar.setValue(0);
            add(progressBar);
            progressBar.setStringPainted(true);
        }

        private void beginLoading() {
            progressBar.setString("Downloading album art...");
            AlbumArtGrabber albumArtGrabber = new AlbumArtGrabber() {            	
                @Override
                protected void done() {
                    // when the album art grabber is done, generate the collages and display that progress
                    if (this.errorCode != null) {
                        // there was an error downloading the images
                        showErrorMessage(this.errorCode);
                    } else {
                        // no problems - go ahead and generate collages
                        generateCollages(progressBar);
                    }
                }
            };

            setupProgressBarChanging(albumArtGrabber);
        }

        private void setupProgressBarChanging(final SwingWorker task) {
            task.execute();

            task.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("progress")) {
                        progressBar.setValue(task.getProgress());
                    }
                }
            });
        }

        private void generateCollages(JProgressBar progressBar) {
            progressBar.setString("Generating collages...");
            progressBar.setValue(0);

            ImageCollageCreator imageCollageCreator = new ImageCollageCreator() {
                @Override
                protected void done() {
                    // when the collages are generated, go the next pane
                    if (this.errorCode != null) {
                        // there was an error generating the collages
                        showErrorMessage(this.errorCode);
                    } else {
                        // no problems
                        nextPanel();
                    }
                }
            };

            setupProgressBarChanging(imageCollageCreator);
        }
    }

    private class WallpaperSetPanel extends JPanel {
        WallpaperSetPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(new JLabel("<html>Your collages are ready!" +
                    "<br>Now you just need to set them as your wallpaper" +
                    "<br>Click the button below, then:" +
                    "<br>1. Set 'background' to 'slideshow'" +
                    "<br>2. Click 'browse', go to 'Musical Wallpaper'" +
                    "<br>3. Click on 'Collages' then click 'Choose folder'" +
                    "<br>And enjoy your favourite music, now your favourite wallpaper!"));
            JButton settingsButton = new JButton("Open wallpaper settings");
            add(settingsButton);
            settingsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Runtime.getRuntime().exec("cmd /c start ms-settings:personalization-background");
                    } catch(IOException exception) {
                        showErrorMessage(exception.getMessage());
                    }
                }
            });
        }
    }


    private void setupPanels() {
        for (JPanel panel : panels) {
            panel.setBorder(new EmptyBorder(30, 30, 30, 30));
        }
    }

    protected void nextPanel() {
        swapPanel(panelPosition + 1);
    }

    protected void previousPanel() {
        swapPanel(panelPosition - 1);
    }

    private void swapPanel(final int newPanelPosition) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                remove(panels[panelPosition]);
                panelPosition = newPanelPosition;
                add(panels[panelPosition]);
                pack();
                invalidate();
                revalidate();

                // start the loading of collages if this is the progress screen
                if (panels[panelPosition] instanceof LoadingScreenPanel) {
                    ((LoadingScreenPanel)panels[panelPosition]).beginLoading();
                }
            }
        });
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        swapPanel(0); // return to the start of the process
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MusicalWallpaperUI musicalWallpaperUI = new MusicalWallpaperUI();
                musicalWallpaperUI.setup();
            }
        });
    }
}
