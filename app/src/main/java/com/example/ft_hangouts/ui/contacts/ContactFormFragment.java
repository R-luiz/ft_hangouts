package com.example.ft_hangouts.ui.contacts;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.ft_hangouts.R;
import com.example.ft_hangouts.model.Contact;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactFormFragment extends Fragment {

    private ContactsViewModel viewModel;
    private TextInputEditText editName, editPhone, editEmail, editAddress, editNotes;
    private CircleImageView contactPhoto;
    private TextView formTitle, formSubtitle;
    private byte[] photoBytes = null;
    private Contact currentContact = null;
    private boolean isEditMode = false;

    // Activity Result Launcher for image picking
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        contactPhoto.setImageBitmap(bitmap);
                        
                        // Convert bitmap to byte array for storage
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        photoBytes = baos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // Permission request launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);

        // Initialize views
        editName = view.findViewById(R.id.edit_name);
        editPhone = view.findViewById(R.id.edit_phone);
        editEmail = view.findViewById(R.id.edit_email);
        editAddress = view.findViewById(R.id.edit_address);
        editNotes = view.findViewById(R.id.edit_notes);
        contactPhoto = view.findViewById(R.id.contact_photo);
        formTitle = view.findViewById(R.id.form_title);
        formSubtitle = view.findViewById(R.id.form_subtitle);
        ImageButton btnChangePhoto = view.findViewById(R.id.btn_change_photo);
        Button btnSave = view.findViewById(R.id.btn_save);

        // Check if we're editing an existing contact
        if (getArguments() != null && getArguments().containsKey("contact")) {
            currentContact = (Contact) getArguments().getSerializable("contact");
            isEditMode = true;
            
            // Update UI for edit mode
            formTitle.setText(R.string.edit_contact);
            formSubtitle.setText("Update the contact information below");
            btnSave.setText(R.string.update_contact);
            
            populateFormFields();
        }

        // Set up photo selection
        btnChangePhoto.setOnClickListener(v -> checkPermissionAndPickImage());

        // Save button
        btnSave.setOnClickListener(v -> saveContact());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Add a delete option to the menu if we're in edit mode
        if (isEditMode) {
            inflater.inflate(R.menu.menu_contact_form, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            deleteContact();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateFormFields() {
        if (currentContact != null) {
            editName.setText(currentContact.getName());
            editPhone.setText(currentContact.getPhoneNumber());
            editEmail.setText(currentContact.getEmail());
            editAddress.setText(currentContact.getAddress());
            editNotes.setText(currentContact.getNotes());
            
            // Set photo if available
            if (currentContact.getPhoto() != null && currentContact.getPhoto().length > 0) {
                photoBytes = currentContact.getPhoto();
                contactPhoto.setImageBitmap(BitmapFactory.decodeByteArray(
                        photoBytes, 0, photoBytes.length));
            }
        }
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void saveContact() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String address = editAddress.getText().toString().trim();
        String notes = editNotes.getText().toString().trim();

        // Validate input
        boolean hasError = false;
        
        if (name.isEmpty()) {
            editName.setError("Name is required");
            hasError = true;
        }
        
        // Basic phone number validation
        if (!phone.isEmpty() && !isValidPhoneNumber(phone)) {
            editPhone.setError("Invalid phone number format");
            hasError = true;
        }
        
        // Basic email validation
        if (!email.isEmpty() && !isValidEmail(email)) {
            editEmail.setError("Invalid email format");
            hasError = true;
        }
        
        if (hasError) {
            return;
        }

        // Create or update contact
        if (isEditMode && currentContact != null) {
            currentContact.setName(name);
            currentContact.setPhoneNumber(phone);
            currentContact.setEmail(email);
            currentContact.setAddress(address);
            currentContact.setNotes(notes);
            
            if (photoBytes != null) {
                currentContact.setPhoto(photoBytes);
            }
            
            viewModel.updateContact(currentContact);
            Toast.makeText(getContext(), "Contact updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Contact newContact = new Contact(name, phone, email, address, photoBytes, notes);
            long contactId = viewModel.addContact(newContact);
            
            if (contactId > 0) {
                Toast.makeText(getContext(), "Contact added successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to add contact. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Navigate back
        Navigation.findNavController(requireView()).navigateUp();
    }
    
    private boolean isValidPhoneNumber(String phone) {
        // Simple validation: at least 7 digits, can contain spaces, dashes, and parentheses
        return phone.replaceAll("[\\s\\-\\(\\)]", "").matches("\\d{7,}");
    }
    
    private boolean isValidEmail(String email) {
        // Simple email validation
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void deleteContact() {
        if (isEditMode && currentContact != null) {
            // Show confirmation dialog
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete " + currentContact.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteContact(currentContact.getId());
                    Toast.makeText(getContext(), "Contact deleted", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        }
    }
}