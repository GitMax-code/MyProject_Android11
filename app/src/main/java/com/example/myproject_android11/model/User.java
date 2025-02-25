package com.example.myproject_android11.model;
import com.google.firebase.firestore.PropertyName;
/*
Annotations @PropertyName : Si vous ne pouvez pas modifier les noms des champs dans votre classe pour
qu'ils correspondent aux noms dans Firestore, vous pouvez utiliser l'annotation
@PropertyName pour spécifier le nom du champ dans Firestore.

        @PropertyName("full_name")
        private String name;

* */
public class User {

    private String id;
    private String name;
    private String email;
    private String commune;
    private boolean admin;

    public User() {
        // Constructeur par défaut requis pour Firestore
    }

    public User(String id, String name, String email, String commune, boolean admin) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.commune = commune;
        this.admin = admin;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    //Setter
    public void setEmail(String email) {
        this.email = email;
    }

    public void setCommune(String commune) {
        this.commune = commune;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
    //Getter

    public String getId(){
        return id;
    }

    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }

    public String getCommune() {
        return commune;
    }

    public boolean getAdmin() {
        return admin;
    }
}
