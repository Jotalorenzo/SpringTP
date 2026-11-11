package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.config.AfipProperties;

public class TestWsaaMain {

    public static void main(String[] args) throws Exception {
        AfipProperties props = new AfipProperties();
        props.setCuit(20421683817L);
        props.setCertPath("certs/certificado_mi_cuit.crt");
        props.setKeyPath("certs/privada_mi_cuit.key");
        props.setPuntoVenta(2);

        AfipWsaaService wsaa = new AfipWsaaService(props);

        System.out.println("Llamando a WSAA homologacion...");
        try {
            String[] resultado = wsaa.obtenerTokenYSign();
            System.out.println("TOKEN: " + resultado[0].substring(0, 40) + "...");
            System.out.println("SIGN:  " + resultado[1].substring(0, 40) + "...");
            System.out.println("OK - Autenticacion exitosa");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}