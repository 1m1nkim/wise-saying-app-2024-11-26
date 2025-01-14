package com.ll.domain.wiseSaying.repository;

import com.ll.domain.wiseSaying.entity.WiseSaying;
import com.ll.global.app.AppConfig;
import com.ll.standard.dto.Pageable;
import com.ll.standard.util.Util;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class WiseSayingFileRepository implements WiseSayingRepository {
    public static String getTableDirPath() {
        return AppConfig.getDbDirPath() + "/wiseSaying";
    }

    public static String getRowFilePath(int id) {
        return getTableDirPath() + "/" + id + ".json";
    }

    public static String getLastIdPath() {
        return getTableDirPath() + "/lastId.txt";
    }

    public static String getArchiveDirPath() {
        return getTableDirPath() + "/data.json";
    }

    @Override
    public WiseSaying save(WiseSaying wiseSaying) {
        boolean isNew = wiseSaying.isNew();

        if (isNew) {
            wiseSaying.setId(getLastId() + 1);
        }

        String jsonStr = wiseSaying.toJsonStr();

        Util.file.set(getRowFilePath(wiseSaying.getId()), jsonStr);

        if (isNew) {
            setLastId(wiseSaying.getId());
        }

        return wiseSaying;
    }

    @Override
    public List<WiseSaying> findAll() {
        try {
            return Util.file.walkRegularFiles(
                            getTableDirPath(),
                            "\\d+\\.json"
                    )
                    .map(path -> Util.file.get(path.toString(), ""))
                    .map(WiseSaying::new)
                    .sorted(Comparator.comparingInt(WiseSaying::getId).reversed())
                    .toList();
        } catch (NoSuchFileException e) {
            return List.of();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        return Util.file.delete(getRowFilePath(id));
    }

    @Override
    public Optional<WiseSaying> findById(int id) {
        String filePath = getRowFilePath(id);

        if (Util.file.notExists(filePath)) {
            return Optional.empty();
        }

        String jsonStr = Util.file.get(filePath, "");

        if (jsonStr.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new WiseSaying(jsonStr));
    }

    public int getLastId() {
        return Util.file.getAsInt(getLastIdPath(), 0);
    }

    private void setLastId(int id) {
        Util.file.set(getLastIdPath(), id);
    }

    public static void dropTable() {
        Util.file.rmdir(WiseSayingFileRepository.getTableDirPath());
    }

    @Override
    public void archive(String archiveDirPath) {
        String jsonStr = Util.json.toString(
                findAll()
                        .stream()
                        .map(WiseSaying::toMap)
                        .toList()
        );

        Util.file.set(archiveDirPath, jsonStr);
    }

    @Override
    public List<WiseSaying> findByKeyword(String keywordType, String keyword) {
        return findAll()
                .stream()
                .filter(wiseSaying -> {
                    if (keywordType.equals("content")) {
                        return wiseSaying.getContent().contains(keyword);
                    }

                    if (keywordType.equals("author")) {
                        return wiseSaying.getAuthor().contains(keyword);
                    }

                    return false;
                })
                .toList();
    }

    @Override
    public void makeSampleData(int items) {
        for (int i = 1; i <= items; i++) {
            save(new WiseSaying(0, "명언 " + i, "작자미상"));
        }
    }

    @Override
    public Pageable<WiseSaying> pageable(int itemsPerPage, int page) {
        List<WiseSaying> all = findAll();

        int totalItems = all.size();

        List<WiseSaying> content = findAll()
                .stream()
                .skip(itemsPerPage * (page - 1))
                .limit(itemsPerPage)
                .toList();

        return new Pageable<>(content, totalItems, itemsPerPage, page);
    }
}
