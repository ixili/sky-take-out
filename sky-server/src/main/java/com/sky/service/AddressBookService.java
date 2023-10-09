package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.AddressBook;

import java.util.List;


/**
 * 地址簿(AddressBook)表服务接口
 *
 * @author lixi
 * @since 2023-10-09 21:51:48
 */
public interface AddressBookService extends IService<AddressBook> {

    List<AddressBook> list(AddressBook addressBook);

//    void save(AddressBook addressBook);

    AddressBook getById(Long id);

    void update(AddressBook addressBook);

    void setDefault(AddressBook addressBook);

    void deleteById(Long id);
}

