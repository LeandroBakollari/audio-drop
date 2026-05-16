package com.audiodrop.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.audiodrop.app.R;
import com.audiodrop.app.model.LibraryFolder;
import com.audiodrop.app.ui.UiFormat;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {
    public interface Listener {
        void onOpenFolder(LibraryFolder folder);

        void onRenameFolder(LibraryFolder folder);

        void onMoveFolder(LibraryFolder folder);

        void onDeleteFolder(LibraryFolder folder);
    }

    public interface CountProvider {
        int itemCount(String folderId);

        int childFolderCount(String folderId);
    }

    private final Listener listener;
    private final CountProvider countProvider;
    private final List<LibraryFolder> folders = new ArrayList<>();

    public FolderAdapter(Listener listener, CountProvider countProvider) {
        this.listener = listener;
        this.countProvider = countProvider;
    }

    public void submitList(List<LibraryFolder> nextFolders) {
        folders.clear();
        folders.addAll(nextFolders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        LibraryFolder folder = folders.get(position);
        holder.iconText.setText(folder.getIconSeed());
        holder.nameText.setText(folder.getName());

        int itemCount = countProvider.itemCount(folder.getId());
        int childCount = countProvider.childFolderCount(folder.getId());
        String fileLabel = UiFormat.countLabel(itemCount, "file", "files");
        String folderLabel = childCount == 0 ? "" : " - " + UiFormat.countLabel(childCount, "folder", "folders");
        holder.countText.setText(fileLabel + folderLabel);

        int actionVisibility = folder.isSystemFolder() ? View.GONE : View.VISIBLE;
        holder.renameButton.setVisibility(actionVisibility);
        holder.moveButton.setVisibility(actionVisibility);
        holder.deleteButton.setVisibility(actionVisibility);

        holder.cardRoot.setOnClickListener(v -> listener.onOpenFolder(folder));
        holder.renameButton.setOnClickListener(v -> listener.onRenameFolder(folder));
        holder.moveButton.setOnClickListener(v -> listener.onMoveFolder(folder));
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteFolder(folder));
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView iconText;
        final TextView nameText;
        final TextView countText;
        final ImageButton renameButton;
        final ImageButton moveButton;
        final ImageButton deleteButton;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            iconText = itemView.findViewById(R.id.iconText);
            nameText = itemView.findViewById(R.id.nameText);
            countText = itemView.findViewById(R.id.countText);
            renameButton = itemView.findViewById(R.id.renameButton);
            moveButton = itemView.findViewById(R.id.moveButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
