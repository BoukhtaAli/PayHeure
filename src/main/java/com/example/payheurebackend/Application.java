package com.example.payheurebackend;

import java.util.Locale;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Application extends SpringBootServletInitializer {

	static {
		// Doit être fixé avant que HSQLDB (moteur interne d'UCanAccess) ne charge la moindre
		// classe : depuis HSQLDB 2.7, "LANGUAGE JAVA" pour une fonction externe est refusé par
		// défaut (sécurité) sauf whitelist explicite. Sans ça, UCanAccess échoue dès la 1ère
		// connexion avec "user lacks privilege or object not found: net.ucanaccess.converters.Functions".
		// Placé dans un bloc statique (et non dans main()) car main() n'est jamais appelée quand
		// l'app est déployée en WAR sur un Tomcat externe : c'est le conteneur qui charge cette
		// classe via SpringBootServletInitializer.
		System.setProperty("hsqldb.method_class_names", "net.ucanaccess.converters.*");
		// Jackcess (utilisé par UCanAccess) dérive l'ordre de tri des index texte de la Locale
		// par défaut de la JVM au moment de la création du fichier .accdb. Sur un poste en
		// locale française (ou toute locale non testée par Jackcess), ça plante avec
		// "Cannot write indexes of this type due to unsupported collating sort order" dès qu'on
		// crée une table avec un index/contrainte. Locale.US est la seule testée de façon fiable.
		Locale.setDefault(Locale.US);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		// Point d'entrée utilisé par le conteneur Servlet (Tomcat externe) quand l'app est
		// déployée en WAR : main() n'est alors jamais appelée.
		return builder.sources(Application.class);
	}

}
