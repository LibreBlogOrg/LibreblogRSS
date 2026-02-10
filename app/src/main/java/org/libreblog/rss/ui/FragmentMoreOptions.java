package org.libreblog.rss.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import org.libreblog.rss.R;
import org.libreblog.rss.core.DbHandler;
import org.libreblog.rss.core.OpmlHandler;
import org.libreblog.rss.core.SourceCrawler;
import org.libreblog.rss.utils.ExtraOptions;
import org.libreblog.rss.utils.Utils;

import java.io.File;
import java.util.List;

public class FragmentMoreOptions extends Fragment {
    private ActivityResultLauncher<Intent> pickOpmlFileLauncher;

    public FragmentMoreOptions() {
        super(R.layout.fragment_more_options);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        pickOpmlFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try (DbHandler db = new DbHandler(view.getContext())) {
                                List<OpmlHandler.SimpleOutline> outlines = OpmlHandler.getSimpleOutlines(getContext(), uri);
                                if (outlines == null) return;

                                for (OpmlHandler.SimpleOutline outline : outlines) {
                                    DbHandler.Source source = new DbHandler.Source();
                                    source.id = outline.xmlUrl;
                                    source.name = outline.text != null && !outline.text.isEmpty() ? outline.text : outline.title;
                                    if (source.name == null || source.name.isEmpty())
                                        source.name = getString(R.string.someone);
                                    source.title = outline.title != null && !outline.title.isEmpty() ? outline.title : source.name;
                                    source.score = outline.score;
                                    source.description = outline.description;
                                    source.type = outline.type;
                                    if (outline.imageUrl != null && !outline.imageUrl.isEmpty()) {
                                        source.image = outline.imageUrl;
                                    }
                                    if (outline.preferredImageUrl != null && !outline.preferredImageUrl.isEmpty()) {
                                        source.preferredImage = outline.preferredImageUrl;
                                    }
                                    if (source.id != null && source.id.startsWith("http")) {
                                        db.putSource(source);
                                        new SourceCrawler(getContext(), null).refreshSource(source.id);
                                    }
                                }
                            }

                            Toast.makeText(view.getContext(), R.string.done, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    Toast.makeText(view.getContext(), R.string.nothing_was_imported, Toast.LENGTH_SHORT).show();
                });

        AppCompatImageButton closeButton = requireActivity().findViewById(R.id.close_more_options_button);
        if (Utils.isDarkTheme(view.getContext()))
            closeButton.setBackgroundColor(Color.rgb(120, 120, 240));
        closeButton.setOnClickListener(v -> exit());

        Button importButton = requireActivity().findViewById(R.id.import_sources_button);
        importButton.setOnClickListener(v -> openOpmlFilePicker());

        Button exportButton = requireActivity().findViewById(R.id.export_sources_button);
        exportButton.setOnClickListener(v -> exportOpmlFile(view));

        Button rateButton = requireActivity().findViewById(R.id.rate_app_button);
        rateButton.setOnClickListener(v -> ExtraOptions.openAppRating(view));

        Button aboutButton = requireActivity().findViewById(R.id.about_button);
        aboutButton.setOnClickListener(v -> ExtraOptions.openAboutPage(view));
    }

    private void openOpmlFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/xml");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/xml", "application/xml", "application/*+xml", "*/*"});
        pickOpmlFileLauncher.launch(intent);
    }

    private void exportOpmlFile(View view) {
        if (view == null) return;

        List<DbHandler.Source> sources;
        try (DbHandler db = new DbHandler(view.getContext())) {
            sources = db.getSources();
        }
        if (sources.isEmpty()) return;

        File opml;
        try {
            opml = OpmlHandler.createOpmlFile(view.getContext(), "subscriptions.opml", sources);
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
            return;
        }

        Uri contentUri = FileProvider.getUriForFile(view.getContext(), "org.libreblog.rss.fileprovider", opml);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/xml");
        share.putExtra(Intent.EXTRA_STREAM, contentUri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, getString(R.string.share_opml)));
    }

    private void exit() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
