package com.pitstop.DataAccessLayer.DTOs;

import java.io.Serializable;

/**
 * Created by Paul on 2/11/2016.
 */
public class Car implements Serializable {
    private String cardId;
    private String make;
    private String ownerId;
    private String dealerShip;
    private String vin;
    private int year;
    private boolean serviceDue;
}
