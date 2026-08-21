package com.example.payheurebackend;

import java.util.Locale;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {

	public static void main(String[] args) {
		// Doit être fixé avant que HSQLDB (moteur interne d'UCanAccess) ne charge la moindre
		// classe : depuis HSQLDB 2.7, "LANGUAGE JAVA" pour une fonction externe est refusé par
		// défaut (sécurité) sauf whitelist explicite. Sans ça, UCanAccess échoue dès la 1ère
		// connexion avec "user lacks privilege or object not found: net.ucanaccess.converters.Functions".
		System.setProperty("hsqldb.method_class_names", "net.ucanaccess.converters.*");
		// Jackcess (utilisé par UCanAccess) dérive l'ordre de tri des index texte de la Locale
		// par défaut de la JVM au moment de la création du fichier .accdb. Sur un poste en
		// locale française (ou toute locale non testée par Jackcess), ça plante avec
		// "Cannot write indexes of this type due to unsupported collating sort order" dès qu'on
		// crée une table avec un index/contrainte. Locale.US est la seule testée de façon fiable.
		Locale.setDefault(Locale.US);
		SpringApplication.run(Application.class, args);
	}

}
