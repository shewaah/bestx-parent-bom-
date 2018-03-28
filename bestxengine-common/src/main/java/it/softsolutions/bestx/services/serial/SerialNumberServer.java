package it.softsolutions.bestx.services.serial;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.dao.SerialNumberDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.services.ServiceListener;

import java.util.Date;

public class SerialNumberServer implements SerialNumberService {

    private SerialNumberDao serialNumberDao;
    private String name = "Serial Number Service";
    private volatile boolean started = true;
    private ServiceListener listener;

    @Override
    public void init() throws BestXException {
        checkPreRequisites();
    }

    public void setSerialNumberDao(SerialNumberDao serialNumberDao) {
        this.serialNumberDao = serialNumberDao;
    }

    @Override
    public long getSerialNumber(String identifier) {
        return serialNumberDao.getNextNumber(identifier);
    }

    @Override
    public long getSerialNumber(String identifier, Date date) throws BestXException {
        return serialNumberDao.getNextNumber(identifier, date);
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (serialNumberDao == null) {
            throw new ObjectNotInitializedException("Serial Number DAO not set");
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isUp() {
        return started;
    }

    @Override
    public void setServiceListener(ServiceListener listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        started = true;
        if (listener != null) {
            listener.onServiceStarted(this);
        }
    }

    @Override
    public void stop() {
        started = false;
        if (listener != null) {
            listener.onServiceStopped(this, "Stop requested from client");
        }
    }

    @Override
    public String getUniqueIdentifier(String identifier, String pattern) throws BestXException {
        if (identifier == null || pattern == null) {
            throw new IllegalArgumentException("Params can't be null");
        }

        long serialNumber = serialNumberDao.getNextNumber(identifier);

        return String.format(pattern, serialNumber);
    }
}
