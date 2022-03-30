## Modules compilation

In order to use the latest version of maven-jahia-plugin, if jahia-parent is still pointing to an older version : 

- Upgrade the maven-jahia-plugin to use the latest version :

```
<properties>
  <jahia.plugin.version>6.2</jahia.plugin.version>
</properties>
``` 

- Then, upgrade the versions of both the maven-bundle-plugin and bndlib that are inherited from the jahia-module pom.
```
<plugin>
    <groupId>org.apache.felix</groupId>
    <artifactId>maven-bundle-plugin</artifactId>
    <version>5.1.2</version>
    <extensions>true</extensions>
    <dependencies>
        <dependency>
            <groupId>biz.aQute.bnd</groupId>
            <artifactId>biz.aQute.bndlib</artifactId>
            <version>6.1.0</version>
        </dependency>
    </dependencies>
    <configuration>
        <instructions>
            <_noimportjava>true</_noimportjava>
        </instructions>
    </configuration>
</plugin> 
```

### Compiling modules with JDK 11

Jahia 8 modules can be compiled to Java 11. To enable this, you will need to use the 6.x version of the maven-jahia-plugin.

Also specify the desired compilation target by adding the following entry:
```
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <release>11</release>
    </configuration>
</plugin>
``` 

Warning: Please be aware that modules relying on OSGI blueprint
or Spring should not be compiled to Java 11. While compilation would succeed,
those modules may not function properly once deployed in Jahia.





