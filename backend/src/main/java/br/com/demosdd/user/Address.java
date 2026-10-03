package br.com.demosdd.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Endereço do usuário. CEP armazenado somente com dígitos. */
@Embeddable
public class Address {

    @Column(name = "cep", nullable = false, length = 8)
    private String cep;

    @Column(name = "street", nullable = false, length = 200)
    private String street;

    @Column(name = "number", nullable = false, length = 20)
    private String number;

    @Column(name = "complement", length = 100)
    private String complement;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    protected Address() {
        // JPA
    }

    public Address(String cep, String street, String number, String complement,
                   String district, String city, String state) {
        this.cep = cep;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.district = district;
        this.city = city;
        this.state = state;
    }

    public String getCep() {
        return cep;
    }

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }

    public String getComplement() {
        return complement;
    }

    public String getDistrict() {
        return district;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }
}
