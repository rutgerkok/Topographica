# Topographica

Live map for Minecraft Spigot servers. View an example map at [mc.marijnk.nl](http://mc.marijnk.nl).

* Google-maps like map for your Minecraft server
* Built-in web server
* Lightweight; requires almost no RAM for rendering
* Saves regions as 256x256 px PNG files
* Live updating of player positions
* Automatic updating of tiles
* Programming API to add markers on the map (bubbles, lines, rectangles, circles, tooltips)

## Installation
Put the plugin in the plugins folder of your server and (re)start the server. Edit the configuration file of the plugin, then restart the server again and then render the whole map using `/topographica fullrender`. You can view the progress using the `/topographica status` command.

## Programming API for markers
This section is for plugin developers that want to make a plugin that draws something on a Topographica map. First, add `Topographica` to the `depends` or `soft-depends` list in the `plugin.yml` file of your plugin. You can get the plugin like this:

```java
Topographica topographica = JavaPlugin.getPlugin(Topographica.class)
```

Then, you can add markers for a world (if the map is active for that world) using the following code:

```java
topographica.getWebWorld(world).ifPresent(webWorld -> {
    // ... add markers here, for example:
    UUID markerId = UUID.randomUUID();
    webWorld.getMarkers().setMarker(markerId,
            Marker.circle(MapLocation.of(200, -30), 10));
});
```

This add a circular marker at (200, -30) with a radius of 10 and a random ID. You can use this ID to remove the marker later on. If you call the `setMarker` method again with the same id, it will replace the previous marker.

It is also possible to customize the look of the marker. Here, we create a magenta circle with a thick, black border and a tooltip with the text:

    Test
    test

The code for this is as follows:

```java
topographica.getWebWorld(world).ifPresent(webWorld -> {
    UUID markerId = UUID.randomUUID();
    webWorld.getMarkers().setMarker(markerId, 
            Marker.circle(MapLocation.of(200, -30), 10)
            .tooltip(HtmlString.fromPlainText("Test\ntest"))
            .withStyle(PolygonStyle.createUsingDefaults()
                    .fillColor(Color.MAGENTA)
                    .strokeColor(Color.BLACK)
                    .strokeWidth(2)));
});
```

Using the other methods in the `Marker` class, you can also draw for example squares, rectangles and lines.
