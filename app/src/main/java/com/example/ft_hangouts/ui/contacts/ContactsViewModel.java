package com.example.ft_hangouts.ui.contacts;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ft_hangouts.db.ContactDbHelper;
import com.example.ft_hangouts.model.Contact;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactsViewModel extends AndroidViewModel {

    private final ContactDbHelper dbHelper;
    private final MutableLiveData<List<Contact>> contacts = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ContactsViewModel(@NonNull Application application) {
        super(application);
        dbHelper = ContactDbHelper.getInstance(application);
        loadContacts();
    }

    public LiveData<List<Contact>> getContacts() {
        return contacts;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void loadContacts() {
        loading.setValue(true);
        executorService.execute(() -> {
            List<Contact> contactList = dbHelper.getAllContacts();
            contacts.postValue(contactList);
            loading.postValue(false);
        });
    }

    // LiveData to track the success of contact operations
    private final MutableLiveData<Long> contactCreationResult = new MutableLiveData<>();
    
    public LiveData<Long> getContactCreationResult() {
        return contactCreationResult;
    }

    public void addContact(Contact contact) {
        loading.setValue(true);
        executorService.execute(() -> {
            try {
                long contactId = dbHelper.addContact(contact);
                if (contactId > 0) {
                    contact.setId(contactId);
                    loadContacts();
                }
                contactCreationResult.postValue(contactId);
            } catch (Exception e) {
                e.printStackTrace();
                contactCreationResult.postValue(-1L);
            } finally {
                loading.postValue(false);
            }
        });
    }

    public void updateContact(Contact contact) {
        executorService.execute(() -> {
            int result = dbHelper.updateContact(contact);
            if (result > 0) {
                loadContacts();
            }
        });
    }

    public void deleteContact(long contactId) {
        executorService.execute(() -> {
            int result = dbHelper.deleteContact(contactId);
            if (result > 0) {
                loadContacts();
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}