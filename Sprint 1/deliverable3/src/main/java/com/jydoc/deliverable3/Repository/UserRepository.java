//This is where the backend interacts with the database to retrieve or add data

package com.jydoc.deliverable3.Repository;
import com.jydoc.deliverable3.Model.UserModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserModel, Long> {



    //Custom Queries
    List<UserModel> findById(int id);  //TODO: Switch this to Long type
    Optional<UserModel> findByEmail(String email);
    List<UserModel> findByFirstName(String firstName);
    List<UserModel> findByLastName(String lastName);
    List<UserModel> findByFirstNameAndLastName(String firstName, String lastName);
    List<UserModel> findAllByEmail(String email);






}
