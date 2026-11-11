package com.example.trabajo_practico_spring.service;

import com.example.trabajo_practico_spring.config.AfipProperties;


public class TestWsaaMain {

    public static void main(String[] args) throws Exception {
        AfipProperties props = new AfipProperties();
        props.setCuit(20416432377L);
        props.setCertPath("C:/Users/pedro/OneDrive/Desktop/utn/SpringTP-main/certs/testspringtp_34954245e2b90f8d.crt");
        props.setKeyPath("C:/Users/pedro/OneDrive/Desktop/utn/SpringTP-main/certs/privada_test.key");
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