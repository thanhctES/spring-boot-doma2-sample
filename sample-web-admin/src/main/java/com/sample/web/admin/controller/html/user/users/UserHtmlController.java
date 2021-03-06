package com.sample.web.admin.controller.html.user.users;

import static com.sample.web.base.WebConst.GLOBAL_MESSAGE;
import static com.sample.web.base.WebConst.MESSAGE_DELETED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sample.domain.dto.common.ID;
import com.sample.domain.dto.common.Pageable;
import com.sample.domain.dto.system.UploadFile;
import com.sample.domain.dto.user.User;
import com.sample.domain.service.user.UserService;
import com.sample.web.base.controller.html.AbstractHtmlController;
import com.sample.web.base.util.MultipartFileUtils;
import com.sample.web.base.view.CsvView;
import com.sample.web.base.view.ExcelView;
import com.sample.web.base.view.PdfView;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * ユーザー管理
 */
@Controller
@RequestMapping("/user/users")
@SessionAttributes(types = { SearchUserForm.class, UserForm.class })
@Slf4j
public class UserHtmlController extends AbstractHtmlController {

    @Autowired
    UserFormValidator userFormValidator;

    @Autowired
    UserService userService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @ModelAttribute("userForm")
    public UserForm userForm() {
        return new UserForm();
    }

    @ModelAttribute("searchUserForm")
    public SearchUserForm searchUserForm() {
        return new SearchUserForm();
    }

    @InitBinder("userForm")
    public void validatorBinder(WebDataBinder binder) {
        binder.addValidators(userFormValidator);
    }

    @Override
    public String getFunctionName() {
        return "A_USER";
    }

    /**
     * 登録画面 初期表示
     *
     * @param form
     * @param model
     * @return
     */
    @GetMapping("/new")
    public String newUser(@ModelAttribute("userForm") UserForm form, Model model) {
        if (!form.isNew()) {
            // SessionAttributeに残っている場合は再生成する
            model.addAttribute("userForm", new UserForm());
        }

        return "modules/user/users/new";
    }

    /**
     * 登録処理
     *
     * @param form
     * @param result
     * @param attributes
     * @return
     */
    @PostMapping("/new")
    public String newUser(@Validated @ModelAttribute("userForm") UserForm form, BindingResult result,
            RedirectAttributes attributes) {
        // 入力チェックエラーがある場合は、元の画面にもどる
        if (result.hasErrors()) {
            setFlashAttributeErrors(attributes, result);
            return "redirect:/user/users/new";
        }

        // 入力値からDTOを作成する
        val inputUser = modelMapper.map(form, User.class);
        val password = form.getPassword();

        // パスワードをハッシュ化する
        inputUser.setPassword(passwordEncoder.encode(password));

        // 登録する
        val createdUser = userService.create(inputUser);

        return "redirect:/user/users/show/" + createdUser.getId().getValue();
    }

    /**
     * 一覧画面 初期表示
     *
     * @param model
     * @return
     */
    @GetMapping("/find")
    public String findUser(@ModelAttribute SearchUserForm form, Model model) {
        // 入力値を詰め替える
        val where = modelMapper.map(form, User.class);

        // 10件区切りで取得する
        val pages = userService.findAll(where, form);

        // 画面に検索結果を渡す
        model.addAttribute("pages", pages);

        return "modules/user/users/find";
    }

    /**
     * 検索結果
     *
     * @param form
     * @param result
     * @param attributes
     * @return
     */
    @PostMapping("/find")
    public String findUser(@Validated @ModelAttribute("searchUserForm") SearchUserForm form, BindingResult result,
            RedirectAttributes attributes) {
        // 入力チェックエラーがある場合は、元の画面にもどる
        if (result.hasErrors()) {
            setFlashAttributeErrors(attributes, result);
            return "redirect:/user/users/find";
        }

        return "redirect:/user/users/find";
    }

    /**
     * 詳細画面
     *
     * @param userId
     * @param model
     * @return
     */
    @GetMapping("/show/{userId}")
    public String showUser(@PathVariable Integer userId, Model model) {
        // 1件取得する
        val user = userService.findById(ID.of(userId));
        model.addAttribute("user", user);

        if (user.getUploadFile() != null) {
            // 添付ファイルを取得する
            val uploadFile = user.getUploadFile();

            // Base64デコードして解凍する
            val base64data = uploadFile.getContent().toBase64();
            val sb = new StringBuilder().append("data:image/png;base64,").append(base64data);

            model.addAttribute("image", sb.toString());
        }

        return "modules/user/users/show";
    }

    /**
     * 編集画面 初期表示
     *
     * @param userId
     * @param form
     * @param model
     * @return
     */
    @GetMapping("/edit/{userId}")
    public String editUser(@PathVariable Integer userId, @ModelAttribute("userForm") UserForm form, Model model) {
        // セッションから取得できる場合は、読み込み直さない
        if (!hasErrors(model)) {
            // 1件取得する
            val user = userService.findById(ID.of(userId));

            // 取得したDtoをFromに詰め替える
            modelMapper.map(user, form);
        }

        return "modules/user/users/new";
    }

    /**
     * 編集画面 更新処理
     *
     * @param form
     * @param result
     * @param userId
     * @param sessionStatus
     * @param attributes
     * @return
     */
    @PostMapping("/edit/{userId}")
    public String editUser(@Validated @ModelAttribute("userForm") UserForm form, BindingResult result,
            @PathVariable Integer userId, SessionStatus sessionStatus, RedirectAttributes attributes) {
        // 入力チェックエラーがある場合は、元の画面にもどる
        if (result.hasErrors()) {
            setFlashAttributeErrors(attributes, result);
            return "redirect:/user/users/edit/" + userId;
        }

        // 更新対象を取得する
        val user = userService.findById(ID.of(userId));

        // 入力値を詰め替える
        modelMapper.map(form, user);

        val image = form.getUserImage();
        if (image != null && !image.isEmpty()) {
            UploadFile uploadFile = user.getUploadFile();
            if (uploadFile == null) {
                uploadFile = new UploadFile();
            }
            MultipartFileUtils.convert(image, uploadFile);
            user.setUploadFile(uploadFile);
        }

        // 更新する
        val updatedUser = userService.update(user);

        // セッションのuserFormをクリアする
        sessionStatus.setComplete();

        return "redirect:/user/users/show/" + updatedUser.getId().getValue();
    }

    /**
     * 削除処理
     *
     * @param userId
     * @param attributes
     * @return
     */
    @PostMapping("/remove/{userId}")
    public String removeUser(@PathVariable Integer userId, RedirectAttributes attributes) {
        // 削除対象を取得する
        val user = userService.findById(ID.of(userId));

        // 論理削除する
        userService.delete(user.getId());

        // 削除成功メッセージ
        attributes.addFlashAttribute(GLOBAL_MESSAGE, getMessage(MESSAGE_DELETED));

        return "redirect:/user/users/find";
    }

    /**
     * CSVダウンロード
     *
     * @param filename
     * @return
     */
    @GetMapping("/download/{filename:.+\\.csv}")
    public ModelAndView downloadCsv(@PathVariable String filename) {
        // 全件取得する
        val users = userService.findAll(new User(), Pageable.NO_LIMIT_PAGEABLE);

        val listType = new TypeToken<List<UserCsv>>() {
        }.getType();
        List<UserCsv> csvList = modelMapper.map(users.getData(), listType);

        // レスポンスを設定する
        val view = new CsvView(UserCsv.class, csvList);
        view.setFilename(filename);

        return new ModelAndView(view);
    }

    /**
     * Excelダウンロード
     *
     * @param filename
     * @return
     */
    @GetMapping(path = "/download/{filename:.+\\.xlsx}")
    public ModelAndView downloadExcel(@PathVariable String filename) {
        // 全件取得する
        val users = userService.findAll(new User(), Pageable.NO_LIMIT_PAGEABLE);
        val view = new ExcelView(filename, new UserExcel());

        Map<String, Object> params = new HashMap<>();
        params.put("data", users.getData());

        return new ModelAndView(view, params);
    }

    /**
     * PDFダウンロード
     *
     * @param filename
     * @return
     */
    @GetMapping(path = "/download/{filename:.+\\.pdf}")
    public ModelAndView downloadPdf(@PathVariable String filename) {
        // 全件取得する
        val users = userService.findAll(new User(), Pageable.NO_LIMIT_PAGEABLE);

        Map<String, Object> params = new HashMap<>();
        params.put("data", users.getData());

        val view = new PdfView("reports/users.jrxml", filename);
        view.setApplicationContext(getApplicationContext());

        return new ModelAndView(view, params);
    }
}
